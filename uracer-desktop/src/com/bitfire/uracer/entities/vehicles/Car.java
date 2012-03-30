package com.bitfire.uracer.entities.vehicles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.carsimulation.CarDescriptor;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInput;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.carsimulation.CarSimulator;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Box2dEntity;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;
import com.bitfire.uracer.utils.VMath;

public class Car extends Box2dEntity {
	protected Recorder recorder;
	protected CarGraphics graphics;

	private CarDescriptor carDesc;
	private CarSimulator carSim;
	private CarInput carInput;
	private CarForces carForces;
	private int impacts;

	private CarInputMode carInputMode;
	private CarType carType;

	private Vector2 startPos;
	private float startOrient;

	private Vector2 tilePosition = new Vector2();

	protected Car( World world, CarGraphics graphics, CarModel model, CarType type, CarInputMode inputMode, Vector2 position, float orientation ) {
		this.graphics = graphics;
		this.recorder = Recorder.instance();
		this.carInputMode = inputMode;
		this.carType = type;
		this.startPos = new Vector2( position );
		this.startOrient = orientation;
		this.impacts = 0;

		carDesc = new CarDescriptor();
		carDesc.carModel.set( model );

		carSim = new CarSimulator( carDesc );
		carInput = new CarInput();
		carForces = new CarForces();

		// body
		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;

		body = world.createBody( bd );
		body.setBullet( true );
		body.setUserData( this );

		setTransform( position, orientation );
		computeTilePosition();
	}

	// factory method
	public static Car createForFactory( World world, CarGraphics graphics, CarModel model, CarType type, CarInputMode inputMode, Vector2 position, float orientation ) {
		Car car = new Car( world, graphics, model, type, inputMode, position, orientation );
		EntityManager.add( car );
		return car;
	}

	public CarType getCarType() {
		return carType;
	}

	public CarInputMode getInputMode() {
		return carInputMode;
	}

	public CarGraphics getGraphics() {
		return graphics;
	}

	public CarDescriptor getCarDescriptor() {
		return carDesc;
	}

	public CarModel getCarModel() {
		return carDesc.carModel;
	}

	public Vector2 getStartPos() {
		return startPos;
	}

	public float getStartOrient() {
		return startOrient;
	}

	public CarSimulator getSimulator() {
		return carSim;
	}

	public void reset() {
		resetPhysics();
		setTransform( startPos, startOrient );
	}

	public void setActive( boolean active, boolean resetPhysics ) {
		if( resetPhysics ) {
			resetPhysics();
		}

		if( active != body.isActive() ) {
			body.setActive( active );
		}
	}

	public boolean isActive() {
		return body.isActive();
	}

	public void resetPhysics() {
		boolean wasActive = isActive();
		if( wasActive ) body.setActive( false );
		carSim.resetPhysics();
		body.setAngularVelocity( 0 );
		body.setLinearVelocity( 0, 0 );
		if( wasActive ) body.setActive( wasActive );
	}

	@Override
	public void setTransform( Vector2 position, float orient_degrees ) {
		super.setTransform( position, orient_degrees );
		carSim.updateHeading( body );
	}

	private float lastTouchAngle;

	private Vector2 touchPos = new Vector2();
	private Vector2 carPos = new Vector2();
	private float invWidth = 1f / Gdx.graphics.getWidth(), invHeight = 1f / Gdx.graphics.getHeight();

	protected CarInput acquireInput() {
		carPos.set( Director.screenPosFor( body ) );

		touchPos.set( Input.getXY() );
		carInput.updated = Input.isTouching();

		if( carInput.updated ) {
			float angle = 0;

			// avoid singularity
			if( (int)-carPos.y + (int)touchPos.y == 0 ) {
				angle = lastTouchAngle;
			}
			else {
				angle = MathUtils.atan2( -carPos.x + touchPos.x, -carPos.y + touchPos.y );
				lastTouchAngle = angle;
			}

			float wrapped = -body.getAngle();

			angle -= AMath.PI;
			angle += wrapped; // to local
			if( angle < 0 ) angle += AMath.TWO_PI;

			angle = -(angle - AMath.TWO_PI);
			if( angle > AMath.PI ) angle = angle - AMath.TWO_PI;

			carInput.steerAngle = angle;

			// normalize and clamp
			touchPos.x *= invWidth;
			touchPos.y *= invHeight;
			carPos.x *= invWidth;
			carPos.y *= invHeight;
			VMath.clamp( touchPos, 0, 1 );
			VMath.clamp( carPos, 0, 1 );

			// compute throttle
			// TODO think, in car::compute_throttle, could be modulated by user's game settings? (multiplier? strength?)
			carInput.throttle = touchPos.dst( carPos ) * 4 * carDesc.carModel.max_force;
			// carInput.throttle = touchPos.dst( carPos ) * 2 * carDesc.carModel.max_force; // x2 = 0<->halfscreen is
			// considered 0<->1
		}

		if( Config.Debug.ApplyFrictionMap ) applyFrictionMap( carInput );

		return carInput;
	}

	private Vector2 offset = new Vector2();
	private WindowedMean frictionMean = new WindowedMean( 16 );

	private void applyFrictionMap( CarInput input ) {
		if( tilePosition.x >= 0 && tilePosition.x < Director.currentLevel.map.width && tilePosition.y >= 0 && tilePosition.y < Director.currentLevel.map.height ) {
			// compute realsize-based pixel offset car-tile (top-left origin)
			float tsx = tilePosition.x * Convert.scaledTilesize;
			float tsy = tilePosition.y * Convert.scaledTilesize;
			offset.set( stateRender.position );
			offset.y = Director.worldSizeScaledPx.y - offset.y;
			offset.x = offset.x - tsx;
			offset.y = offset.y - tsy;
			offset.mul( Convert.invScaledTilesize ).mul( Director.currentLevel.map.tileWidth );

			TiledLayer layerTrack = MapUtils.getLayer( MapUtils.LayerTrack );
			int id = layerTrack.tiles[(int)tilePosition.y][(int)tilePosition.x] - 1;

			// int xOnMap = (id %4) * 224 + (int)offset.x;
			// int yOnMap = (int)( id/4f ) * 224 + (int)offset.y;

			// bit twiddling, faster versions
			int xOnMap = (id & 3) * (int)Director.currentLevel.map.tileWidth + (int)offset.x;
			int yOnMap = (id >> 2) * (int)Director.currentLevel.map.tileWidth + (int)offset.y;

			int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
			frictionMean.addValue( (pixel == -256 ? 0 : -1) );
			// System.out.println(pixel);

			if( frictionMean.getMean() < -0.4 && carDesc.velocity_wc.len2() > 10 ) {
				carDesc.velocity_wc.mul( 0.975f );
			}
		}
		else {
			System.out.println( "Car out of map" );
		}
	}

	private void computeTilePosition() {
		tilePosition.set( Convert.pxToTile( stateRender.position.x, stateRender.position.y ) );
		VMath.truncateToInt( tilePosition );
	}

	public Vector2 getTilePosition() {
		return tilePosition;
	}

	private long start_timer = 0;
	private boolean start_decrease = false;

	private void handleImpactFeedback() {
		// process impact feedback
		while( impacts > 0 ) {
			impacts--;
			carDesc.velocity_wc.set( body.getLinearVelocity() ).mul( Director.gameplaySettings.linearVelocityAfterFeedback );
			carDesc.angularvelocity = -body.getAngularVelocity() * 0.85f;
			start_decrease = true;
		}
	}

	private void handleDecrease( CarInput input ) {
		if( start_decrease || (System.nanoTime() - start_timer < 250000000L) ) {
			if( start_decrease ) {
				start_decrease = false;
				start_timer = System.nanoTime();
			}

			input.throttle *= Director.gameplaySettings.throttleDampingAfterFeedback;
		}
	}

	/** Subclasses, such as the GhostCar, will override this method
	 * to feed forces from external sources, such as Replay data stored
	 * elsewhere.
	 *
	 * @param forces computed forces will be returned by filling this data structure. */
	protected void onComputeCarForces( CarForces forces ) {
		carInput = acquireInput();

		// handle decrease queued from previous step
		handleDecrease( carInput );

		carSim.applyInput( carInput );
		carSim.step( body );

		// record computed forces, if recording is enabled
		forces.velocity_x = carDesc.velocity_wc.x;
		forces.velocity_y = carDesc.velocity_wc.y;
		forces.angularVelocity = carDesc.angularvelocity;

		if( recorder.isRecording() ) {
			recorder.add( forces );
		}
	}


	public void onCollide(Fixture other, Vector2 normalImpulses) {
		impacts++;

		// update DriftInfo in case of collision
		if( carInputMode == CarInputMode.InputFromPlayer ) {
			CarSoundManager.carImpacted( normalImpulses.len() );
			GameData.driftState.invalidateByCollision();
		}

	}

	@Override
	public void onBeforePhysicsSubstep() {
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
	public void onAfterPhysicsSubstep() {
		super.onAfterPhysicsSubstep();

		// inspect impact feedback, accumulate vel/ang velocities
		handleImpactFeedback();

		computeTilePosition();
	}

	@Override
	public void onBeforeRender( float temporalAliasingFactor ) {
		super.onBeforeRender( temporalAliasingFactor );
		stateRender.toPixels();
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		graphics.render( batch, stateRender );
	}

	@Override
	public void onDebug() {
		if( carInputMode != CarInputMode.InputFromPlayer ) return;

		if( Config.Graphics.RenderPlayerDebugInfo ) {
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

			Debug.drawString( "on tile " + tilePosition, 0, 0 );
		}
	}
}