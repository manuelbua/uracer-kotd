package com.bitfire.uracer.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.actors.CarState;
import com.bitfire.uracer.game.actors.CarType;
import com.bitfire.uracer.game.logic.Input;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.WorldDefs.TileLayer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.VMath;

public class PlayerCar extends Car {

	// car forces simulator
	private CarSimulator carSim = null;
	private CarDescriptor carDesc = null;

	// input
	private Input input = null;
	private CarInput carInput = null;
	private float lastTouchAngle;
	private Vector2 touchPos = new Vector2();
	private Vector2 carPos = new Vector2();
	private final float invWidth = 1f / Gdx.graphics.getWidth(), invHeight = 1f / Gdx.graphics.getHeight();
	private WindowedMean frictionMean = new WindowedMean( 10 );

	// damping values
	private float dampLinearVelocityAF = 0;
	private float dampThrottleAF = 0;
	private float dampFriction = 0;

	// states
	public CarState carState = null;
	public PlayerDriftState driftState = null;

	public PlayerCar( GameWorld gameWorld, CarModel model, Aspect aspect ) {
		super( gameWorld, CarType.PlayerCar, InputMode.InputFromPlayer, GameRendererEvent.Order.DEFAULT, model, aspect, true );
		carInput = new CarInput();
		impacts = 0;

		carDesc = new CarDescriptor();
		carDesc.carModel.set( model );
		carSim = new CarSimulator( carDesc );
		renderer.setAlpha( 1 );

		// states
		this.carState = new CarState( gameWorld, this );
		this.driftState = new PlayerDriftState( this );
	}

	@Override
	public void dispose() {
		super.dispose();
		driftState.dispose();
	}

	// use strictly for debug purposes *ONLY*!
	public CarDescriptor getCarDescriptor() {
		return carDesc;
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

	/** Sets the input system this PlayerCar will use to check for input events */
	public void setInputSystem( Input input ) {
		this.input = input;
	}

	/** After processing collision's feedback this damping will be applied
	 * to the car's linear velocity. */
	public void setDampingLinearVelocityAF( float damping ) {
		dampLinearVelocityAF = damping;
	}

	/** After processing collision's feedback this damping will be applied
	 * to the car's input throttle */
	public void setDampingThrottleAF( float damping ) {
		dampThrottleAF = damping;
	}

	/** When the player's car is off-track this damping will be applied
	 * to the car's linear velocity */
	public void setDampingFriction( float damping ) {
		dampFriction = damping;
	}

	protected CarInput acquireInput() {
		if( input == null ) {
			carInput.reset();
			return carInput;
		}

		boolean workInNormalized = false;
		carPos.set( GameRenderer.ScreenUtils.screenPosForMt( body.getPosition() ) );
		touchPos.set( input.getXY() );

		carInput.updated = input.isTouching();

		if( carInput.updated ) {
			float angle = 0;

			if( workInNormalized ) {
				carPos.x *= invWidth;
				carPos.y *= invHeight;
				touchPos.x *= invWidth;
				touchPos.y *= invHeight;
				VMath.clamp( touchPos, 0, 1 );
				VMath.clamp( carPos, 0, 1 );

				carInput.throttle = touchPos.dst( carPos ) * 4 * carDesc.carModel.max_force;
				carInput.steerAngle = transformSteerAngle( (float)Math.atan2( -carPos.x + touchPos.x, -carPos.y + touchPos.y ) );
			} else {

				if( (int)-carPos.y + (int)touchPos.y == 0 ) {
					// avoid singularity
					angle = lastTouchAngle;
				} else {
					angle = MathUtils.atan2( -carPos.x + touchPos.x, -carPos.y + touchPos.y );
					lastTouchAngle = angle;
				}

				carInput.steerAngle = transformSteerAngle( angle );

				carPos.x *= invWidth;
				carPos.y *= invHeight;
				touchPos.x *= invWidth;
				touchPos.y *= invHeight;
				VMath.clamp( touchPos, 0, 1 );
				VMath.clamp( carPos, 0, 1 );
				carInput.throttle = touchPos.dst( carPos ) * 4 * carDesc.carModel.max_force;
			}
		}

		return carInput;
	}

	private float transformSteerAngle( float angle ) {
		float transformed = angle;

		transformed -= AMath.PI;
		transformed += -body.getAngle(); // to local
		if( transformed < 0 ) {
			transformed += AMath.TWO_PI;
		}

		transformed = -(transformed - AMath.TWO_PI);
		if( transformed > AMath.PI ) {
			transformed = transformed - AMath.TWO_PI;
		}

		return transformed;
	}

	@Override
	protected void onComputeCarForces( CarForces forces ) {
		carInput = acquireInput();

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

		carState.update( carDesc );
		driftState.update( carSim.lateralForceFront.y, carSim.lateralForceRear.y, carDesc.velocity_wc.len() );

		if( Config.Debug.ApplyCarFrictionFromMap ) {
			updateCarFriction();
			applyFriction();
		}
	}

	private Vector2 offset = new Vector2();

	private void updateCarFriction() {
		Vector2 tilePosition = carState.tilePosition;

		if( gameWorld.isValidTilePosition( tilePosition ) ) {
			// compute realsize-based pixel offset car-tile (top-left origin)
			float scaledTileSize = gameWorld.getTileSizeScaled();
			float tsx = tilePosition.x * scaledTileSize;
			float tsy = tilePosition.y * scaledTileSize;
			offset.set( Convert.mt2px( getBody().getPosition() ) );
			offset.y = gameWorld.worldSizeScaledPx.y - offset.y;
			offset.x = offset.x - tsx;
			offset.y = offset.y - tsy;
			offset.mul( gameWorld.getTileSizeInvScaled() ).mul( gameWorld.map.tileWidth );

			TiledLayer layerTrack = gameWorld.getLayer( TileLayer.Track );
			int id = layerTrack.tiles[(int)tilePosition.y][(int)tilePosition.x] - 1;

//			int xOnMap = (id %4) * (int)gameWorld.map.tileWidth + (int)offset.x;
//			int yOnMap = (int)( id/4f ) * (int)gameWorld.map.tileWidth + (int)offset.y;

			// bit twiddling, faster version
			int xOnMap = (id & 3) * (int)gameWorld.map.tileWidth + (int)offset.x;
			int yOnMap = (id >> 2) * (int)gameWorld.map.tileWidth + (int)offset.y;

			int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
			frictionMean.addValue( (pixel == -256 ? 0 : -1) );

//			Gdx.app.log( "PlayerCar", "xmap=" + xOnMap + ", ymap=" + yOnMap );
//			Gdx.app.log( "PlayerCar", "mean=" + frictionMean.getMean() + ", pixel=" + pixel + ", xmap=" + xOnMap + ", ymap=" + yOnMap );
//			Gdx.app.log( "PlayerCar", "id=" + id );

		} else {
			Gdx.app.log( "PlayerCar", "PlayerCar out of map!" );
		}
	}

	private void applyFriction() {
		// FIXME, move these hard-coded values out of here
		if( frictionMean.getMean() < -0.1 && carDesc.velocity_wc.len2() > 10 ) {
			carDesc.velocity_wc.mul( dampFriction );
//			Gdx.app.log( "PlayerCar", "Friction applied." );
		}
	}

	private long start_timer = 0;
	private boolean start_decrease = false;

	private void handleImpactFeedback() {
		// process impact feedback
		while( impacts > 0 ) {
			impacts--;
			carDesc.velocity_wc.set( body.getLinearVelocity() ).mul( dampLinearVelocityAF );
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

			input.throttle *= dampThrottleAF;
		}
	}
}
