package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.input.Input;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.SpriteBatchUtils;
import com.bitfire.uracer.utils.VMath;

public class Car extends Box2DEntity {
	public enum InputMode {
		NoInput, InputFromPlayer, InputFromReplay
	}

	public enum Aspect {
		OldSkool, OldSkool2
	}

	protected CarRenderer graphics;

	private CarDescriptor carDesc = null;
	private CarSimulator carSim = null;
	private CarInput carInput = null;
	private CarForces carForces = null;
	private Input inputSystem = null;
	private int impacts = 0;

	private float linearVelocityDampAF = 0.99f;
	private float throttleDampAF = 0.99f;

	protected InputMode inputMode = InputMode.NoInput;
	private Aspect aspect = Aspect.OldSkool;

	public Car( World box2dWorld, CarRenderer graphics, CarModel model, Aspect type ) {
		super( box2dWorld );
		this.graphics = graphics;
		this.aspect = type;
		this.impacts = 0;
		this.inputSystem = null;
		this.inputMode = InputMode.NoInput;

		carDesc = new CarDescriptor();
		carDesc.carModel.set( model );

		carSim = new CarSimulator( carDesc );
		carInput = new CarInput();
		carForces = new CarForces();

		// body
		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;

		body = box2dWorld.createBody( bd );
		body.setBullet( true );
		body.setUserData( this );

		Gdx.app.log( getClass().getSimpleName(), "Input mode is " + this.inputMode.toString() );
	}

	public Aspect getCarAspect() {
		return aspect;
	}

	public InputMode getInputMode() {
		return inputMode;
	}

	public CarRenderer getGraphics() {
		return graphics;
	}

	public CarDescriptor getCarDescriptor() {
		return carDesc;
	}

	public CarModel getCarModel() {
		return carDesc.carModel;
	}

	public Vector2 getLateralForceFront() {
		return carSim.lateralForceFront;
	}

	public Vector2 getLateralForceRear() {
		return carSim.lateralForceRear;
	}

	/** After processing collision's feedback this damping will be applied
	 * to the car's linear velocity. */
	public void setLinearVelocityDampingAF( float damping ) {
		linearVelocityDampAF = damping;
	}

	/** After processing collision's feedback this damping will be applied
	 * to the car's input throttle */
	public void setThrottleDampingAF( float damping ) {
		throttleDampAF = damping;
	}

	public void setInputSystem( Input inputSystem ) {
		this.inputSystem = inputSystem;
		this.inputMode = (inputSystem != null ? InputMode.InputFromPlayer : InputMode.NoInput);
		Gdx.app.log( getClass().getSimpleName(), "Switched input mode to " + this.inputMode.toString() );
	}

	private WindowedMean frictionMean = new WindowedMean( 16 );

	public void setFriction( float value ) {
		frictionMean.addValue( value );
	}

	public void reset() {
		resetPhysics();
		frictionMean.clear();
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

	private void resetPhysics() {
		boolean wasActive = isActive();

		if( wasActive ) {
			body.setActive( false );
		}

		carSim.resetPhysics();
		body.setAngularVelocity( 0 );
		body.setLinearVelocity( 0, 0 );

		if( wasActive ) {
			body.setActive( wasActive );
		}
	}

	@Override
	public void setTransform( Vector2 position, float orient_degrees ) {
		super.setTransform( position, orient_degrees );
		// carSim.updateHeading( body.getAngle() );
		// computeTilePosition();
	}

	private float lastTouchAngle;

	private Vector2 touchPos = new Vector2();
	private Vector2 carPos = new Vector2();
	private float invWidth = 1f / Gdx.graphics.getWidth(), invHeight = 1f / Gdx.graphics.getHeight();

	protected CarInput acquireInput() {
		carPos.set( Director.screenPosFor( body ) );

		touchPos.set( inputSystem.getXY() );
		carInput.updated = inputSystem.isTouching();

		if( carInput.updated ) {
			float angle = 0;

			// avoid singularity
			if( (int)-carPos.y + (int)touchPos.y == 0 ) {
				angle = lastTouchAngle;
			} else {
				angle = MathUtils.atan2( -carPos.x + touchPos.x, -carPos.y + touchPos.y );
				lastTouchAngle = angle;
			}

			float wrapped = -body.getAngle();

			angle -= AMath.PI;
			angle += wrapped; // to local
			if( angle < 0 ) {
				angle += AMath.TWO_PI;
			}

			angle = -(angle - AMath.TWO_PI);
			if( angle > AMath.PI ) {
				angle = angle - AMath.TWO_PI;
			}

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

		if( Config.Debug.ApplyCarFrictionFromMap ) {
			applyFriction();
		}

		return carInput;
	}

	private void applyFriction() {
		if( frictionMean.getMean() < -0.4 && carDesc.velocity_wc.len2() > 10 ) {
			carDesc.velocity_wc.mul( 0.975f );
		}
	}

	private long start_timer = 0;
	private boolean start_decrease = false;

	private void handleImpactFeedback() {
		// process impact feedback
		while( impacts > 0 ) {
			impacts--;
			carDesc.velocity_wc.set( body.getLinearVelocity() ).mul( linearVelocityDampAF );
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

			input.throttle *= throttleDampAF;
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
		carSim.step( Config.Physics.PhysicsDt, body.getAngle() );

		// record computed forces, if recording is enabled
		forces.velocity_x = carDesc.velocity_wc.x;
		forces.velocity_y = carDesc.velocity_wc.y;
		forces.angularVelocity = carDesc.angularvelocity;

		GameEvents.carEvent.data.setForces( forces );
		GameEvents.carEvent.trigger( this, CarEvent.Type.onComputeForces );
	}

	public void onCollide( Fixture other, Vector2 normalImpulses ) {
		impacts++;

		GameEvents.carEvent.data.setCollisionData( other, normalImpulses );
		GameEvents.carEvent.trigger( this, CarEvent.Type.onCollision );
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

		// computeTilePosition();
	}

	@Override
	public void onRender( SpriteBatch batch ) {
		graphics.render( batch, stateRender );
	}

	@Override
	public void onDebug( SpriteBatch batch ) {
		if( inputMode != InputMode.InputFromPlayer ) {
			return;
		}

		if( Config.Graphics.RenderPlayerDebugInfo ) {
			SpriteBatchUtils.drawString( batch, "vel_wc len =" + carDesc.velocity_wc.len(), 0, 13 );
			SpriteBatchUtils.drawString( batch, "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
			SpriteBatchUtils.drawString( batch, "steerangle=" + carDesc.steerangle, 0, 27 );
			SpriteBatchUtils.drawString( batch, "throttle=" + carDesc.throttle, 0, 34 );
			SpriteBatchUtils.drawString( batch, "screen x=" + Director.screenPosFor( body ).x + ",y=" + Director.screenPosFor( body ).y, 0, 80 );
			SpriteBatchUtils.drawString( batch, "world-mt x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, 87 );
			SpriteBatchUtils.drawString( batch, "world-px x=" + Convert.mt2px( body.getPosition().x ) + ",y=" + Convert.mt2px( body.getPosition().y ), 0, 93 );
			// Debug.drawString( "dir worldsize x=" + Director.worldSizeScaledPx.x + ",y=" +
			// Director.worldSizeScaledPx.y, 0, 100 );
			// Debug.drawString( "dir bounds x=" + Director.boundsPx.x + ",y=" + Director.boundsPx.width, 0, 107 );
			SpriteBatchUtils.drawString( batch, "orient=" + body.getAngle(), 0, 114 );
			SpriteBatchUtils.drawString( batch, "render.interp=" + (state().position.x + "," + state().position.y), 0, 121 );

			// BatchUtils.drawString( batch, "on tile " + tilePosition, 0, 0 );
		}
	}
}