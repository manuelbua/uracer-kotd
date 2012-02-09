package com.bitfire.uracer.entities.vehicles;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.carsimulation.CarDescriptor;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInput;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.carsimulation.CarSimulator;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.entities.Box2dEntity;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

public class Car extends Box2dEntity
{
	protected Recorder recorder;
	protected CarGraphics graphics;

	private CarDescriptor carDesc;
	private CarSimulator carSim;
	private CarInput carInput;
	private CarForces carForces;
	private ArrayList<Float> impactFeedback;

	private CarInputMode carInputMode;
	private CarType carType;

	private Vector2 startPos;
	private float startOrient;

	protected Car( CarGraphics graphics, CarModel model, CarType type, CarInputMode inputMode, Vector2 position, float orientation )
	{
		this.graphics = graphics;
		this.impactFeedback = new ArrayList<Float>();
		this.recorder = Recorder.instance();
		this.carInputMode = inputMode;
		this.carType = type;
		this.startPos = new Vector2(position);
		this.startOrient = orientation;

		carDesc = new CarDescriptor();
		carDesc.carModel.set( model );

		carSim = new CarSimulator( carDesc );
		carInput = new CarInput();
		carForces = new CarForces();

		// body
		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;

		body = Physics.world.createBody( bd );
		body.setBullet( true );
		body.setUserData( this );

		setTransform( position, orientation );
	}

	// factory method
	public static Car createForFactory( CarGraphics graphics, CarModel model, CarType type, CarInputMode inputMode, Vector2 position, float orientation )
	{
		Car car = new Car( graphics, model, type, inputMode, position, orientation );
		EntityManager.add( car );
		return car;
	}

	public CarType getCarType()
	{
		return carType;
	}

	public CarInputMode getInputMode()
	{
		return carInputMode;
	}

	public CarGraphics getGraphics()
	{
		return graphics;
	}

	public CarDescriptor getCarDescriptor()
	{
		return carDesc;
	}

	public CarModel getCarModel()
	{
		return carDesc.carModel;
	}

	public Vector2 getStartPos()
	{
		return startPos;
	}

	public float getStartOrient()
	{
		return startOrient;
	}

	public CarSimulator getSimulator()
	{
		return carSim;
	}

	public void reset()
	{
		resetPhysics();
		setTransform( startPos, startOrient );
	}

	public void setActive(boolean active, boolean resetPhysics)
	{
		if(resetPhysics)
		{
			resetPhysics();
		}

		if( active != body.isActive() )
		{
			body.setActive( active );
		}
	}

	public boolean isActive()
	{
		return body.isActive();
	}

	public void resetPhysics()
	{
		boolean wasActive = isActive();
		if(wasActive) body.setActive( false );
		carSim.resetPhysics();
		body.setAngularVelocity( 0 );
		body.setLinearVelocity( 0, 0 );
		if(wasActive) body.setActive( wasActive );
	}

	@Override
	public void setTransform( Vector2 position, float orient_degrees )
	{
		super.setTransform( position, orient_degrees );
		carSim.updateHeading( body );
	}

	private float lastTouchAngle;

	protected CarInput acquireInput()
	{
		Vector2 carScreenPos = Director.screenPosFor( body );

		Vector2 touchPos = Input.getXY();
		carInput.updated = Input.isTouching();

		if( carInput.updated )
		{
			float angle = 0;

			// avoid singularity
			if( (int)-carScreenPos.y + (int)touchPos.y == 0 )
			{
				angle = lastTouchAngle;
			} else
			{
				angle = MathUtils.atan2( -carScreenPos.x + touchPos.x, -carScreenPos.y + touchPos.y );
				lastTouchAngle = angle;
			}

			float wrapped = -body.getAngle();

			angle -= AMath.PI;
			angle += wrapped; // to local
			if( angle < 0 )
				angle += AMath.TWO_PI;

			angle = -(angle - AMath.TWO_PI);
			if( angle > AMath.PI )
				angle = angle - AMath.TWO_PI;

			carInput.steerAngle = angle;

			// compute throttle
			carInput.throttle = touchPos.dst( carScreenPos );
		}

		applyFriction(carInput);
		return carInput;
	}

	private Vector2 carTileAt = new Vector2(), currPos = new Vector2();
	private WindowedMean frictionMean = new WindowedMean( 16 );
	private void applyFriction(CarInput input)
	{
		currPos.set(pos());
//		currPos.set(this.state().position);

		carTileAt.set( Convert.pxToTile( currPos.x, currPos.y ) );

		if( carTileAt.x > 0 && carTileAt.x < Director.currentLevel.map.width &&
			carTileAt.y > 0 && carTileAt.y < Director.currentLevel.map.height )
			{
				// compute realsize-based pixel offset car-tile (top-left origin)
				float tsx = carTileAt.x * Convert.scaledTilesize;
				float tsy = carTileAt.y * Convert.scaledTilesize;
				Vector2 offset = currPos;
				offset.y = Director.worldSizeScaledPx.y - offset.y;
				offset.x = offset.x - tsx;
				offset.y = offset.y - tsy;
				offset.mul(Convert.invScaledTilesize).mul(Director.currentLevel.map.tileWidth);

				TiledLayer layerTrack = MapUtils.getLayer( MapUtils.LayerTrack );
				int id = layerTrack.tiles[(int)carTileAt.y][(int)carTileAt.x] - 1;
				int xOnMap = (id %4) * 224 + (int)offset.x;
				int yOnMap = (int)( id/4f ) * 224 + (int)offset.y;

				int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
				int val = ( pixel == -256 ? 0 : -1 );
				frictionMean.addValue( val );
//				System.out.println(frictionMean.getMean());
//				System.out.println(id+"-"+xOnMap + ", " + yOnMap + "=" + pixel);

				if( frictionMean.getMean() < -0.4 && carDesc.velocity_wc.len2() > 10 )
				{
//					input.throttle = 5f;
					carDesc.velocity_wc.mul( 0.975f );
				}
			}
	}

	public void addImpactFeedback( float feedback )
	{
		impactFeedback.add( feedback );
	}

	private long start_timer = 0;
	private boolean start_decrease = false;
	private float prevStrength = 0;
	private void handleImpactFeedback()
	{
		// process impact feedback
		float impact = 0f;
		boolean hasImpact = false;
		while( impactFeedback.size() > 0 )
		{
			float impulse = impactFeedback.remove( 0 );
			impact += impulse;

			carDesc.velocity_wc.set( body.getLinearVelocity() ).mul( Director.gameplaySettings.linearVelocityAfterFeedback );
			carDesc.angularvelocity = -body.getAngularVelocity() * 0.85f;

			start_decrease = true;
			hasImpact = true;
		}


		if( (carInputMode == CarInputMode.InputFromPlayer) && PostProcessor.hasEffect() && hasImpact )
		{
			float strength = AMath.lerp( prevStrength, impact*0.005f, 0.05f );
			prevStrength = strength;
			PostProcessor.getEffect().addStrength( strength );
		}
	}

	private void handleDecrease(CarInput input)
	{
		if(start_decrease || (System.nanoTime() - start_timer < 250000000L) )
		{
			if(start_decrease)
			{
				start_decrease = false;
				start_timer = System.nanoTime();
			}

			input.throttle *= Director.gameplaySettings.throttleDampingAfterFeedback;
		}
	}

	/**
	 * Subclasses, such as the GhostCar, will override this method
	 * to feed forces from external sources, such as Replay data stored
	 * elsewhere.
	 *
	 * @param forces computed forces will be returned by filling this data structure.
	 */
	protected void onComputeCarForces( CarForces forces )
	{
		carInput = acquireInput();

		// handle decrease queued from previous step
		handleDecrease( carInput );

		carSim.applyInput( carInput );
		carSim.step( body );

		// record computed forces, if recording is enabled
		forces.velocity_x = carDesc.velocity_wc.x;
		forces.velocity_y = carDesc.velocity_wc.y;
		forces.angularVelocity = carDesc.angularvelocity;

		if( recorder.isRecording() )
		{
			recorder.add( forces );
		}
	}

	@Override
	public void onBeforePhysicsSubstep()
	{
		super.onBeforePhysicsSubstep();

		onComputeCarForces( carForces );

		// update the car descriptor with newly computed forces
		carDesc.velocity_wc.set( carForces.velocity_x, carForces.velocity_y );
		carDesc.angularvelocity = carForces.angularVelocity;

		// set computed/replayed velocities
		body.setAwake( true );
		body.setLinearVelocity( carDesc.velocity_wc );
		body.setAngularVelocity( -carDesc.angularvelocity );
	}

	@Override
	public void onAfterPhysicsSubstep()
	{
		super.onAfterPhysicsSubstep();

		// inspect impact feedback, accumulate vel/ang velocities
		handleImpactFeedback();
	}

	@Override
	public void onBeforeRender( float temporalAliasingFactor )
	{
		super.onBeforeRender( temporalAliasingFactor );
		stateRender.toPixels();
	}

	@Override
	public void onRender( SpriteBatch batch )
	{
		graphics.render( batch, stateRender );
	}

	private Vector2 tmp = new Vector2();

	@Override
	public void onDebug()
	{
		if( carInputMode != CarInputMode.InputFromPlayer )
			return;

		if( Config.Graphics.RenderPlayerDebugInfo )
		{
			Debug.drawString( "vel_wc len =" + carDesc.velocity_wc.len(), 0, 13 );
			Debug.drawString( "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
			Debug.drawString( "steerangle=" + carDesc.steerangle, 0, 27 );
			Debug.drawString( "throttle=" + carDesc.throttle, 0, 34 );
			Debug.drawString( "screen x=" + Director.screenPosFor( body ).x + ",y=" + Director.screenPosFor( body ).y, 0, 80 );
			Debug.drawString( "world-mt x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, 87 );
			Debug.drawString( "world-px x=" + Convert.mt2px( body.getPosition().x ) + ",y=" + Convert.mt2px( body.getPosition().y ), 0, 93 );
			Debug.drawString( "dir worldsize x=" + Director.worldSizeScaledPx.x + ",y=" + Director.worldSizeScaledPx.y, 0, 100 );
			Debug.drawString( "dir bounds x=" + Director.boundsPx.x + ",y=" + Director.boundsPx.width, 0, 107 );
			Debug.drawString( "orient=" + body.getAngle(), 0, 114 );
			Debug.drawString( "render.interp=" + (state().position.x + "," + state().position.y), 0, 121 );

			tmp.set( Convert.pxToTile( stateRender.position.x, stateRender.position.y ) );
			Debug.drawString( "on tile " + tmp, 0, 0 );
		}
	}
}