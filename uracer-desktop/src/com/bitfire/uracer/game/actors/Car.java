package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
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
	private float carTraveledDistance = 0;
	private int sums = 0;

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

	public int getSums() {
		return sums;
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
		sums = 0;
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

		// // accumulte it
		// carTraveledDistance += distmp.len();
		// sums++;

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
	public void onAfterPhysicsSubstep() {
		super.onAfterPhysicsSubstep();

		// compute traveled distance, in meters
		distmp.set( body.getPosition() );
		distmp.sub( previousPosition );

		// filter out zero distance
		float dist = AMath.fixup( distmp.len() );
		if( !AMath.isZero( dist ) ) {
			// accumulte it
			carTraveledDistance += dist;
			sums++;
		}
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		renderer.render( batch, stateRender );
	}
}