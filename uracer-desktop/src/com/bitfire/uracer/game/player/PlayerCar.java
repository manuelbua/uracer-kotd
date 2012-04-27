package com.bitfire.uracer.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.Director;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.actors.CarState;
import com.bitfire.uracer.game.input.Input;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.SpriteBatchUtils;
import com.bitfire.uracer.utils.VMath;

public class PlayerCar extends Car {

	// car forces simulator
	private CarSimulator carSim = null;
	private CarDescriptor carDesc = null;

	// input
	private CarInput carInput = null;
	private Input inputSystem = null;
	private float lastTouchAngle;
	private Vector2 touchPos = new Vector2();
	private Vector2 carPos = new Vector2();
	private float invWidth = 1f / Gdx.graphics.getWidth(), invHeight = 1f / Gdx.graphics.getHeight();
	private WindowedMean frictionMean = new WindowedMean( 16 );

	// input damping
	private float linearVelocityDampAF = 0.99f;
	private float throttleDampAF = 0.99f;

	// states
	public CarState carState = null;
	public PlayerDriftState driftState = null;

	public PlayerCar( World box2dWorld, GameWorld gameWorld, CarModel model, Aspect aspect ) {
		super( box2dWorld, gameWorld, model, aspect );
		carInput = new CarInput();
		inputSystem = null;
		impacts = 0;

		carDesc = new CarDescriptor();
		carDesc.carModel.set( model );
		carSim = new CarSimulator( carDesc );

		// states
		this.carState = new CarState( gameWorld, this );
		this.driftState = new PlayerDriftState( this );
	}

	@Override
	public void reset() {
		super.reset();
		frictionMean.clear();
		carState.reset();
		driftState.reset();
	}

	@Override
	protected void resetPhysics() {
		super.resetPhysics();
		carSim.resetPhysics();
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

	public void setFriction( float value ) {
		frictionMean.addValue( value );
	}

	// input data for this car cames from an Input object
	public void setInputSystem( Input inputSystem ) {
		this.inputSystem = inputSystem;
		this.inputMode = (inputSystem != null ? InputMode.InputFromPlayer : InputMode.NoInput);
		Gdx.app.log( getClass().getSimpleName(), "Switched input mode to " + this.inputMode.toString() );
	}

	protected CarInput acquireInput() {
		// FIXME check if inputSystem is valid!
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
			carInput.throttle = touchPos.dst( carPos ) * 4 * carDesc.carModel.max_force;
			// carInput.throttle = touchPos.dst( carPos ) * 2 * carDesc.carModel.max_force; // x2 = 0<->halfscreen is considered 0<->1
		}

		return carInput;
	}

	private void applyFriction() {
		if( frictionMean.getMean() < -0.4 && carDesc.velocity_wc.len2() > 10 ) {
			carDesc.velocity_wc.mul( 0.975f );
		}
	}

	@Override
	protected void onComputeCarForces( CarForces forces ) {
		carInput = acquireInput();

		if( Config.Debug.ApplyCarFrictionFromMap ) {
			applyFriction();
		}

		// handle decrease queued from previous step
		handleDecrease( carInput );

		carSim.applyInput( carInput );
		carSim.step( Config.Physics.PhysicsDt, body.getAngle() );

		// update computed forces
		forces.velocity_x = carDesc.velocity_wc.x;
		forces.velocity_y = carDesc.velocity_wc.y;
		forces.angularVelocity = carDesc.angularvelocity;

		// update the car descriptor (car simulator data) with newly computed forces
		// (no interface on carsim for performance reasons!)
		carDesc.velocity_wc.set( forces.velocity_x, forces.velocity_y );
		carDesc.angularvelocity = forces.angularVelocity;
	}

	@Override
	public void onAfterPhysicsSubstep() {
		super.onAfterPhysicsSubstep();

		// inspect impact feedback, accumulate vel/ang velocities
		handleImpactFeedback();

		carState.update( carDesc.velocity_wc.len2(), carDesc.throttle );
		driftState.update( carSim.lateralForceFront.y, carSim.lateralForceRear.y, carDesc.velocity_wc.len() );
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

	@Override
	public void onDebug( SpriteBatch batch ) {
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
