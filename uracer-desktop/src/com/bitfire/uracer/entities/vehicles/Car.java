package com.bitfire.uracer.entities.vehicles;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.b2dEntity;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.simulations.car.CarDescriptor;
import com.bitfire.uracer.simulations.car.CarInput;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.simulations.car.CarSimulator;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class Car extends b2dEntity
{
	// public OrthographicAlignedMesh mesh;
	protected CarGraphics graphics;
	private boolean isPlayer;

	protected Vector2 originalPosition = new Vector2();
	protected float originalOrientation;
	public CarDescriptor carDesc;
	protected CarSimulator carSim;
	public CarInput carInput;
	private ArrayList<CarInput> cil;
	public ArrayList<Float> impactFeedback;

	protected Car( CarGraphics graphics, CarModel model, Vector2 position, float orientation, boolean isPlayer )
	{
		this.isPlayer = isPlayer;
		this.originalPosition.set( position );
		this.originalOrientation = orientation;
		this.graphics = graphics;
		this.cil = new ArrayList<CarInput>( 2500 );
		this.impactFeedback = new ArrayList<Float>();

		carDesc = new CarDescriptor();
		carDesc.carModel.set( model );

		carSim = new CarSimulator( carDesc );
		carInput = new CarInput();

		// body
		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;

		body = Physics.world.createBody( bd );

		// mass
//		MassData md = new MassData();
//		md.mass = carDesc.carModel.mass;
//		md.I = carDesc.carModel.inertia;
//		md.center.set( 0, 0 );
//		body.setMassData( md );

		body.setBullet( true );
		body.setUserData( this );

		setTransform( position, orientation );

		// mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.obj",
		// "data/3d/palm.png", new Vector2( 1, 1 ) );
		// mesh.setScale( 3f );
		// mesh.setPositionOffsetPixels( 0, 0 );
	}

	// factory method
	public static Car createForFactory( CarGraphics graphics, CarModel model, Vector2 position, float orientation, boolean isPlayer)
	{
		Car car = new Car( graphics, model, position, orientation, isPlayer );
		EntityManager.add( car );
		return car;
	}

	public void resetPhysics()
	{
		carSim.resetPhysics();
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

			// damp the throttle
			if( !AMath.isZero( carInput.throttle ) )
			{
				carInput.throttle *= 1.5f;
			}
		}

		// if( isRecording )
		// {
		// CarInput ci = new CarInput( carInput );
		// cil.add( ci );
		// }

		return carInput;
	}

	// private boolean isRecording = false;
	//
	// public void record(boolean rec)
	// {
	// if(rec) cil.clear();
	// isRecording = rec;
	// }
	//
	// public boolean isRecording()
	// {
	// return this.isRecording;
	// }
	//
	//
	// public int recordIndex()
	// {
	// return cil.size();
	// }
	//
	// public ArrayList<CarInput> getReplay()
	// {
	// return cil;
	// }


	private long start_timer = 0;
	private boolean start_decrease = false;
	private void handleImpactFeedback()
	{
//		if( impactFeedback.size() > 0 && PostProcessor.hasEffect() )
//			PostProcessor.getEffect().setEnabled( true );
//		else PostProcessor.getEffect().setEnabled( false );

		// process impact feedback
		float impact = 0f;
		boolean hasImpact = false;
		while( impactFeedback.size() > 0 )
		{
			float impulse = impactFeedback.remove( 0 );
			impact += impulse;

			carDesc.velocity_wc.set( body.getLinearVelocity() ).mul( Director.gameplaySettings.linearVelocityAfterFeedback );
			carDesc.angularvelocity = -body.getAngularVelocity() * 0.85f;

//			carDesc.velocity_wc.set( body.getLinearVelocity() );
//			carDesc.angularvelocity = -body.getAngularVelocity();

			start_decrease = true;
			hasImpact = true;
		}

		if( PostProcessor.hasEffect() && hasImpact )
		{
//			PostProcessor.getEffect().setStrength( impact * 0.0005f );
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

	@Override
	public void onBeforePhysicsSubstep()
	{
		super.onBeforePhysicsSubstep();

		if(isPlayer) carInput = acquireInput();

		// handle decrease queued from previous step
		handleDecrease( carInput );

		carSim.applyInput( carInput );
		carSim.step( body );

		// set velocities
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
		// reverse y
		// mesh.setPosition( stateRender.position.x * Config.TileMapZoomFactor, -stateRender.position.y * Config.TileMapZoomFactor );
		// mesh.setRotation( stateRender.orientation, 0, 1, 0 );

		graphics.render( batch, stateRender );
	}

	private Vector2 tmp = new Vector2();

	@Override
	public void onDebug()
	{
		if(!isPlayer)
			return;

//		Debug.drawString( "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
//		Debug.drawString( "steerangle=" + carDesc.steerangle, 0, 27 );
//		Debug.drawString( "throttle=" + carDesc.throttle, 0, 34 );
//		Debug.drawString( "tx=" + Input.getXY().x + ",ty=" + Input.getXY().y, 0, 41 );
//		Debug.drawString( "screen x=" + Director.screenPosFor( body ).x + ",y=" + Director.screenPosFor( body ).y, 0, 80 );
//		Debug.drawString( "world x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, 87 );
//		Debug.drawString( "orient=" + body.getAngle(), 0, 94 );
//
//		Debug.drawString( "input count = " + cil.size(), 0, 106 );
//		// Debug.drawString( "REC = " + isRecording, 0, 118 );

		tmp.set( Convert.pxToTile( stateRender.position.x, stateRender.position.y ) );
		Debug.drawString( "on tile " + tmp, 0, 0 );
	}
}