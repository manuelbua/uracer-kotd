package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;

public abstract class Car extends Box2DEntity {
	public enum InputMode {
		NoInput, InputFromPlayer, InputFromReplay
	}

	public enum Aspect {
		OldSkool, OldSkool2
	}

	/* event */
	public CarEvent event = null;

	protected GameWorld gameWorld;
	protected CarModel model;
	protected CarRenderer renderer;

	protected int impacts = 0;

	private CarForces carForces = new CarForces();
	private Vector2 carForcesVelocity = new Vector2();

	// distance
	private Vector2 previousPosition = new Vector2();
	private Vector2 distmp = new Vector2();

	// the car's traveled distance so far, in meters
	private float carTraveledDistance = 0;
	private int accuDistCount = 0;

	// the car's traveled distance performed in the last dt, in meters
	private float carTraveledDistanceDt = 0;

	// the car's average speed, in meters/sec, so far.
	private float carAvgSpeedMtSec = 0;
	private float accuSpeed = 0;
	private int accuSpeedCount = 0;

	// the car's instant speed, in meters/sec
	private float carInstantSpeedMtSec = 0;

	private Aspect aspect = Aspect.OldSkool;
	protected InputMode inputMode = InputMode.NoInput;

	public Car( World box2dWorld, GameWorld gameWorld, CarModel model, Aspect aspect ) {
		super( box2dWorld );
		this.event = new CarEvent( this );
		this.gameWorld = gameWorld;
		this.renderer = new CarRenderer( model, aspect );
		this.aspect = aspect;
		this.impacts = 0;
		this.model = model;
		this.inputMode = InputMode.NoInput;
		this.carTraveledDistance = 0;
		this.carTraveledDistanceDt = 0;
		this.accuDistCount = 0;
		this.accuSpeed = 0;
		this.accuSpeedCount = 0;

		// body
		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;

		body = box2dWorld.createBody( bd );
		body.setBullet( true );
		body.setUserData( this );

		Gdx.app.log( getClass().getSimpleName(), "Input mode is " + this.inputMode.toString() );
	}

	/** Subclasses will feed forces to the simulator, such as Replay data stored
	 * elsewhere or from user input.
	 *
	 * @param forces computed forces shall be returned by filling the passed data structure. */
	protected abstract void onComputeCarForces( CarForces forces );

	public Aspect getAspect() {
		return aspect;
	}

	public CarModel getCarModel() {
		return model;
	}

	public InputMode getInputMode() {
		return inputMode;
	}

	public CarRenderer getRenderer() {
		return renderer;
	}

	/** Returns the traveled distance, in meters, so far.
	 * Calling reset() will also reset the traveled distance. */
	public float getTraveledDistance() {
		return carTraveledDistance;
	}

	/** Returns the traveled distance, in meters, performed in the last dt */
	public float getTraveledDistanceDt() {
		return carTraveledDistanceDt;
	}

	/** Returns the average speed, in meters/s, so far. */
	public float getAverageSpeed() {
		return carAvgSpeedMtSec;
	}

	/** Returns the instant speed, in meters/s */
	public float getInstantSpeed() {
		return carInstantSpeedMtSec;
	}

	public int getAccuDistCount() {
		return accuDistCount;
	}

	public int getAccuSpeedCount() {
		return accuSpeedCount;
	}

	public void setActive( boolean active ) {
		if( active != body.isActive() ) {
			body.setActive( active );
		}
	}

	public boolean isActive() {
		return body.isActive();
	}

	public void reset() {
		resetPhysics();
		resetTraveledDistance();
	}

	public void resetTraveledDistance() {
		carTraveledDistance = 0;
		carTraveledDistanceDt = 0;
		accuDistCount = 0;
		accuSpeed = 0;
		accuSpeedCount = 0;
	}

	protected void resetPhysics() {
		boolean wasActive = isActive();

		if( wasActive ) {
			body.setActive( false );
		}

		body.setAngularVelocity( 0 );
		body.setLinearVelocity( 0, 0 );

		if( wasActive ) {
			body.setActive( wasActive );
		}
	}

	public void onCollide( Fixture other, Vector2 normalImpulses ) {
		impacts++;

		event.data.setCollisionData( other, normalImpulses );
		event.trigger( this, CarEvent.Type.onCollision );
	}

	@Override
	public void setTransform( Vector2 position, float orient ) {
		super.setTransform( position, orient );

		// keeps track of the previous position to be able
		// to compute the cumulative traveled distance
		previousPosition.set( body.getPosition() );
	}

	@Override
	public void onBeforePhysicsSubstep() {
		super.onBeforePhysicsSubstep();

		// keeps track of the previous position to be able
		// to compute the cumulative traveled distance
		previousPosition.set( body.getPosition() );

		// reset mt/dt
		carTraveledDistanceDt = 0;

		// reset instant speed
		carInstantSpeedMtSec = 0;

		// let's subclasses behave as needed, ask them to fill carForces with new data
		onComputeCarForces( carForces );

		// trigger event, new forces have been computed
		event.data.setForces( carForces );
		event.trigger( this, CarEvent.Type.onComputeForces );

		// FIXME is it really necessary? that's an expensive jni call..
		body.setAwake( true );

		// put newly computed forces into the system
		carForcesVelocity.set( carForces.velocity_x, carForces.velocity_y );
		body.setLinearVelocity( carForcesVelocity );
		body.setAngularVelocity( -carForces.angularVelocity );
	}

	@Override
	public strictfp void onAfterPhysicsSubstep() {
		super.onAfterPhysicsSubstep();

		// compute traveled distance, in meters

		distmp.set( body.getPosition() );
		distmp.sub( previousPosition );

		// filter out zero distance
		float dist = AMath.fixup( distmp.len() );
		if( !AMath.isZero( dist ) ) {
			// keeps track of it
			carTraveledDistanceDt = dist;

			// accumulate distance it
			carTraveledDistance += dist;
			accuDistCount++;
		}

		// compute instant speed, these should be valid w/ a stddev of ~0.001
		carInstantSpeedMtSec = AMath.fixup( carTraveledDistanceDt * Config.Physics.PhysicsTimestepHz );
		accuSpeed += carInstantSpeedMtSec;
		accuSpeedCount++;

		// compute average speed, these should be valid w/ a stddev of ~0.001
		carAvgSpeedMtSec = AMath.fixup( accuSpeed / (float)accuSpeedCount );
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		renderer.render( batch, stateRender );
	}
}