package com.bitfire.uracer.entities;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.simulation.CarDescriptor;
import com.bitfire.uracer.simulation.CarInput;
import com.bitfire.uracer.simulation.CarSimulator;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class Car extends b2dEntity
{
	// public OrthographicAlignedMesh mesh;
	protected Sprite sprite;
	private boolean isPlayer;

	private Vector2 impactVelocity = new Vector2();
	protected Vector2 originalPosition = new Vector2();
	protected float originalOrientation;
	public CarDescriptor carDesc;
	protected CarSimulator carSim;
	public CarInput carInput;
	private ArrayList<CarInput> cil;

	private PolygonShape shape;

	protected Car( Vector2 position, float orientation, boolean isPlayer )
	{
		this.isPlayer = isPlayer;
		this.originalPosition.set( position );
		this.originalOrientation = orientation;
		this.cil = new ArrayList<CarInput>( 2500 );

		carDesc = CarDescriptor.create();
		carDesc.carModel.toModel2();
//		carDesc.carModel.toBlackCar();

		carSim = new CarSimulator( carDesc );
		carInput = new CarInput();

		impactVelocity.set( 0, 0 );

		Vector2 half = new Vector2( (carDesc.carModel.width / 2f), (carDesc.carModel.length / 2f) );

		shape = new PolygonShape();
		shape.setAsBox( half.x, half.y );

		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;

		body = Physics.world.createBody( bd );

		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = carDesc.carModel.density;
		fd.friction = carDesc.carModel.friction;
		fd.restitution = carDesc.carModel.restitution;

		/* Fixture f = */body.createFixture( fd );
		// f->SetUserData( (void*)( iIsPlayer ? (unsigned int)ShapeCarPlayer :
		// (unsigned int)ShapeCar ) );
		MassData md = new MassData();
		md.mass = carDesc.carModel.mass;
		md.I = carDesc.carModel.inertia;
		md.center.set( 0, 0 );
		body.setMassData( md );
		body.setBullet( true );

		// build gfx
		sprite = new Sprite();
		sprite.setRegion( Art.cars.findRegion( "electron" ) );
//		sprite.setRegion( Art.blackCar );
		sprite.setSize( Convert.mt2px( half.x * 2 ), Convert.mt2px( half.y * 2 ) );
//		sprite.setScale( 1f );
		sprite.setOrigin( sprite.getWidth() / 2, sprite.getHeight() / 2 );

		setTransform( position, orientation );

		// mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.obj",
		// "data/3d/palm.png", new Vector2( 1, 1 ) );
		// mesh.setScale( 3f );
		// mesh.setPositionOffsetPixels( 0, 0 );
	}

	// factory method
	public static Car create( Vector2 position, float orientation )
	{
		Car car = new Car( position, orientation, true );
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

	@Override
	public void onBeforePhysicsSubstep()
	{
		super.onBeforePhysicsSubstep();

		carSim.applyInput( acquireInput() );
		carSim.step( body );

		// setup forces
		body.setAwake( true );
		body.setLinearVelocity( carDesc.velocity_wc );
		body.setAngularVelocity( -carDesc.angularvelocity );
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

		sprite.setPosition( stateRender.position.x - sprite.getOriginX(), stateRender.position.y - sprite.getOriginY() );
		sprite.setRotation( stateRender.orientation );
		sprite.draw( batch );
	}

	private Vector2 tmp = new Vector2();

	@Override
	public void onDebug()
	{
		Debug.drawString( "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
		Debug.drawString( "steerangle=" + carDesc.steerangle, 0, 27 );
		Debug.drawString( "throttle=" + carDesc.throttle, 0, 34 );
		Debug.drawString( "tx=" + Input.getXY().x + ",ty=" + Input.getXY().y, 0, 41 );
		Debug.drawString( "screen x=" + Director.screenPosFor( body ).x + ",y=" + Director.screenPosFor( body ).y, 0, 80 );
		Debug.drawString( "world x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, 87 );
		Debug.drawString( "orient=" + body.getAngle(), 0, 94 );

		Debug.drawString( "input count = " + cil.size(), 0, 106 );
		// Debug.drawString( "REC = " + isRecording, 0, 118 );

		tmp.set( Convert.pxToTile( stateRender.position.x, stateRender.position.y ) );
		Debug.drawString( "on tile " + tmp, 0, 140 );

	}
}