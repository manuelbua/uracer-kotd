package com.bitfire.uracer.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.actors.CarPreset;
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

	// private ScalingStrategy strategy;

	// car forces simulator
	private CarSimulator carSim = null;
	private CarDescriptor carDesc = null;

	// input
	private Input input = null;
	private CarInput carInput = null;
	private Vector2 touchPos = new Vector2();
	private Vector2 carPos = new Vector2();
	private final float invWidth = 1f / Gdx.graphics.getWidth(), invHeight = 1f / Gdx.graphics.getHeight();
	// private float scaleInputX, scaleInputY;
	private WindowedMean frictionMean = new WindowedMean( 10 );

	// damping values
	private float dampFriction = 0;

	// states
	public CarState carState = null;
	public PlayerDriftState driftState = null;

	public PlayerCar( GameWorld gameWorld, CarPreset.Type presetType ) {
		super( gameWorld, CarType.PlayerCar, InputMode.InputFromPlayer, GameRendererEvent.Order.MINUS_1, presetType, true );
		carInput = new CarInput();
		impacts = 0;

		// strategy = gameWorld.scalingStrategy;
		carDesc = new CarDescriptor( preset.model );
		carSim = new CarSimulator( carDesc );
		renderer.setAlpha( 1 );

		// precompute relaxing factors for user input coordinates
		// scaleInputX = invWidth * gameWorld.scalingStrategy.referenceResolution.x;
		// scaleInputY = invHeight * gameWorld.scalingStrategy.referenceResolution.y;

		// states
		this.carState = new CarState( gameWorld, this );
		this.driftState = new PlayerDriftState( this );

		// set physical properties
		dampFriction = GameplaySettings.DampingFriction;
	}

	@Override
	public void dispose() {
		super.dispose();
		driftState.dispose();
		frictionMean = null;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	// use strictly for debug purposes *ONLY*!
	public CarDescriptor getCarDescriptor() {
		return carDesc;
	}

	@Override
	public void resetPhysics() {
		super.resetPhysics();
		carSim.resetPhysics();
		frictionMean.clear();
		carState.reset();
		driftState.reset();
	}

	/** Sets the input system this PlayerCar will use to check for input events */
	public void setInputSystem( Input input ) {
		this.input = input;
	}

	/** When the player's car is off-track this damping will be applied
	 * to the car's linear velocity */
//	public void setDampingFriction( float damping ) {
//		dampFriction = damping;
//	}

	protected CarInput acquireInput() {
		if( input == null ) {
			carInput.reset();
			return carInput;
		}

		carPos.set( GameRenderer.ScreenUtils.worldMtToScreen( body.getPosition() ) );
		touchPos.set( input.getXY() );

		carInput.updated = input.isTouching();
		// Gdx.app.log( "PlayerCar", "carpos=" + carPos.toString() + ", cursor=" + touchPos.toString() );

		if( carInput.updated ) {

			// compute steer angle first
			carInput.steerAngle = transformSteerAngle( (float)Math.atan2( -carPos.x + touchPos.x, -carPos.y + touchPos.y ) );

			// normalize positions and compute final throttle
			touchPos.x *= invWidth;
			touchPos.y *= invHeight;
			carPos.x *= invWidth;
			carPos.y *= invHeight;
			VMath.clamp( touchPos, 0, 1 );
			VMath.clamp( carPos, 0, 1 );

			carInput.throttle = touchPos.dst( carPos ) * 4 * preset.model.max_force;
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

		return transformed;// * 0.75f;
	}

	@Override
	protected void onComputeCarForces( CarForces forces ) {
		carInput = acquireInput();

		// handle decrease scheduled from previous step
		// handleDecrease( carInput );
		handleImpactFeedback();

		carSim.applyInput( carInput );
		carSim.step( Config.Physics.PhysicsDt, body.getAngle() );

		// update computed forces
		forces.velocity_x = carDesc.velocity_wc.x;
		forces.velocity_y = carDesc.velocity_wc.y;
		forces.angularVelocity = carDesc.angularvelocity;
	}

	@Override
	public void onSubstepCompleted() {
		// inspect impact feedback, accumulate vel/ang velocities
		// handleImpactFeedback();

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

			// int xOnMap = (id %4) * (int)gameWorld.map.tileWidth + (int)offset.x;
			// int yOnMap = (int)( id/4f ) * (int)gameWorld.map.tileWidth + (int)offset.y;

			// bit twiddling, faster version
			int xOnMap = (id & 3) * (int)gameWorld.map.tileWidth + (int)offset.x;
			int yOnMap = (id >> 2) * (int)gameWorld.map.tileWidth + (int)offset.y;

			int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
			frictionMean.addValue( (pixel == -256 ? 0 : -1) );

			// Gdx.app.log( "PlayerCar", "xmap=" + xOnMap + ", ymap=" + yOnMap );
			// Gdx.app.log( "PlayerCar", "mean=" + frictionMean.getMean() + ", pixel=" + pixel + ", xmap=" + xOnMap +
			// ", ymap=" + yOnMap );
			// Gdx.app.log( "PlayerCar", "id=" + id );

		} else {
			Gdx.app.log( "PlayerCar", "PlayerCar out of map!" );
		}
	}

	private void applyFriction() {
		// FIXME, move these hard-coded values out of here
		if( frictionMean.getMean() < -0.3 && carDesc.velocity_wc.len2() > 10 ) {
			carDesc.velocity_wc.mul( dampFriction );
		}
	}

	private void handleImpactFeedback() {
		// process impact feedback
		while( impacts > 0 ) {
			impacts--;

			// feed back the result to the car simulator
			carDesc.velocity_wc.set( body.getLinearVelocity() );
			carDesc.angularvelocity = -body.getAngularVelocity();
		}
	}
}
