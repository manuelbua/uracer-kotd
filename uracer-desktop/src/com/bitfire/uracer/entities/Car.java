package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.simulation.CarDescriptor;
import com.bitfire.uracer.simulation.CarSimulator;
import com.bitfire.uracer.utils.AMath;

public class Car extends b2dEntity
{
	private Sprite sprite;
	private boolean isPlayer;

	private Vector2 impactVelocity = new Vector2();
	private CarDescriptor carDesc;
	private CarSimulator carSim;

	private PolygonShape shape;

	private Car( Vector2 worldPos, boolean isPlayer )
	{
		this.isPlayer = isPlayer;
		carDesc = CarDescriptor.create();
//		carDesc.carModel.toModel1();

		carSim = new CarSimulator( carDesc );
		impactVelocity.set( 0, 0 );

		Vector2 half = new Vector2( (carDesc.carModel.width / 2f), (carDesc.carModel.length / 2f) );

		shape = new PolygonShape();
		shape.setAsBox( half.x, half.y );

		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;
		bd.position.set( worldPos.x, worldPos.y );

		body = Physics.world.createBody( bd );

		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = carDesc.carModel.density;
		fd.friction = carDesc.carModel.friction;
		fd.restitution = carDesc.carModel.restitution;

		/*Fixture f =*/ body.createFixture( fd );
		// f->SetUserData( (void*)( iIsPlayer ? (unsigned int)ShapeCarPlayer : (unsigned int)ShapeCar ) );
		MassData md = new MassData();
		md.mass = carDesc.carModel.mass;
		md.I = carDesc.carModel.inertia;
		md.center.set( 0, 0 );
		body.setMassData( md );
		body.setBullet( true );

		// build gfx
		sprite = new Sprite();
		sprite.setRegion( Art.cars.findRegion("electron") );
		sprite.setSize( Physics.mt2px(half.x*2), Physics.mt2px(half.y*2) );
		sprite.setOrigin( sprite.getWidth() / 2, sprite.getHeight() / 2 );
		System.out.println("sprite_size = " + sprite.getWidth() + ", " + sprite.getHeight());
	}

	// factory method
	public static Car create( Vector2 position, boolean isPlayer )
	{
		Car car = new Car( position, isPlayer );
		EntityManager.add( car );
		return car;
	}

	public void reset()
	{
		carSim.resetPhysics();
	}

//	public void setPosition( Vector2 worldPos )
//	{
//		setTransform( worldPos, body.getAngle() );
//	}

	// fixme
//	public void setOrientation( float orientRadians )
//	{
//		setTransform( body.getPosition(), orientRadians );
//	}

	// fixme
	public void setTransform( Vector2 worldPos, float orientRadians )
	{
		carDesc.angle = orientRadians;
		carDesc.wrapped_angle = AMath.wrap2PI( orientRadians );
		body.setTransform( worldPos, -orientRadians );
		carSim.updateHeading();
		System.out.println("car_xform=" + body.getPosition().toString());
	}

	private Vector2 wp = new Vector2();
	public Vector2 getWorldPos()
	{
		wp.set( body.getPosition() );
		return wp;
	}

	@Override
	public void onBeforePhysicsSubstep()
	{
		super.onBeforePhysicsSubstep();

		if( isPlayer )
			carSim.acquireInput( body );
		carSim.applyInput();
		carSim.step();

		body.setAwake( true );
		body.setLinearVelocity( carDesc.velocity_wc );
		body.setAngularVelocity( -carDesc.angularvelocity );
//		body.setTransform( body.getPosition(), -carDesc.angle );
//		setOrientation( carDesc.angle );
	}

	@Override
	public void onRender( SpriteBatch batch )
	{
		sprite.setPosition( stateRender.position.x - sprite.getOriginX(), stateRender.position.y - sprite.getOriginY() );
		sprite.setRotation( stateRender.orientation );
		sprite.draw( batch );
//		System.out.println(stateRender);
	}

	public void debug( Screen screen, SpriteBatch batch )
	{
		// dbg
		screen.drawString( "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
		screen.drawString( "steerangle=" + carDesc.steerangle, 0, 27 );
		screen.drawString( "throttle=" + carDesc.throttle, 0, 34 );
		screen.drawString( "tx="+Input.getXY().x + ",ty=" +Input.getXY().y, 0, 41 );
		screen.drawString( "angle=" + body.getAngle(), 0, 55 );
		screen.drawString( "cd.angle=" + carDesc.angle, 0, 62 );
		screen.drawString( "cd.wangle=" + carDesc.wrapped_angle, 0, 69 );
		screen.drawString( "screen x="+Director.screenPosFor( body ).x + ",y=" +Director.screenPosFor( body ).y, 0, 80 );
		screen.drawString( "world x="+body.getPosition().x + ",y=" +body.getPosition().y, 0, 87 );
	}
}
