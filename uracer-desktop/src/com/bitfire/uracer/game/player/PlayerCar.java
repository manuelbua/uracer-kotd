
package com.bitfire.uracer.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.IntMap;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Input.MouseButton;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.CarState;
import com.bitfire.uracer.game.actors.CarType;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.WorldDefs.Layer;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.Timer;
import com.bitfire.uracer.utils.VMath;

public class PlayerCar extends Car {
	// car simulation
	private CarSimulator carSim = null;
	private CarDescriptor carDesc = null;

	// input
	private Input input = null;
	private CarInput carInput = null;
	private Vector2 touchPos = new Vector2();
	private Vector2 carPos = new Vector2();
	private final float invWidth, invHeight;
	private WindowedMean frictionMean = new WindowedMean(10);

	private IntMap<Timer> keytimer = new IntMap<Timer>(3);

	// states
	public CarState carState = null;
	public DriftState driftState = null;
	public boolean isThrottling = false;

	public PlayerCar (GameWorld gameWorld, GameLogic gameLogic, CarPreset.Type presetType) {
		super(gameWorld, gameLogic, CarType.PlayerCar, InputMode.InputFromPlayer, presetType, true);
		carInput = new CarInput();
		impacts = 0;

		invWidth = 1f / (float)Config.Graphics.ReferenceScreenWidth;
		invHeight = 1f / (float)Config.Graphics.ReferenceScreenHeight;

		// strategy = gameWorld.scalingStrategy;
		carDesc = new CarDescriptor(preset.model);
		carSim = new CarSimulator(carDesc);
		stillModel.setAlpha(1);

		// states
		this.carState = new CarState(gameWorld, this);
		this.driftState = new DriftState(this);

		keytimer.put(Keys.UP, new Timer());
		keytimer.put(Keys.LEFT, new Timer());
		keytimer.put(Keys.RIGHT, new Timer());
	}

	@Override
	public void dispose () {
		super.dispose();
		driftState.dispose();
		frictionMean = null;
	}

	@Override
	public boolean isVisible () {
		return true;
	}

	// use strictly for debug purposes *ONLY*!
	public CarDescriptor getCarDescriptor () {
		return carDesc;
	}

	@Override
	public float getSteerAngleRads () {
		return carDesc.steerangle;
	};

	public CarSimulator getSimulator () {
		return carSim;
	}

	@Override
	public void resetPhysics () {
		super.resetPhysics();
		carSim.resetPhysics();
		frictionMean.clear();
		carState.reset();
		driftState.reset();
	}

	public void reset () {
		resetPhysics();
		resetDistanceAndSpeed(true, true);
		setWorldPosMt(gameWorld.playerStart.position, gameWorld.playerStart.orientation);
		gameTrack.resetTrackState(this);
	}

	public boolean isOutOfTrack () {
		return isOutOfTrack;
	}

	/** Sets the input system this PlayerCar will use to check for input events */
	public void setInputSystem (Input input) {
		this.input = input;
	}

	@Override
	public strictfp void onCollide (Fixture other, Vector2 normalImpulses, float frontRatio) {
		super.onCollide(other, normalImpulses, frontRatio);
		if (driftState.isDrifting) {
			driftState.invalidateByCollision();
		}
	}

	protected CarInput acquireInput () {
		if (input == null) {
			carInput.reset();
			return carInput;
		}

		boolean inputFromKeyboard = !URacer.Game.getInputSystem().isTouching();
		final float speed = carState.currSpeedFactor;
		isThrottling = false;

		carInput.brake = 0;

		if (!inputFromKeyboard) {

			// mouse/pointer input

			carPos.set(GameRenderer.ScreenUtils.worldMtToScreen(body.getPosition()));
			touchPos.set(input.getXY());

			carInput.updated = input.isTouching() && input.isTouchedInBounds(MouseButton.Left);
			// Gdx.app.log("PlayerCar", "carpos=" + carPos.toString() + ", cursor=" + touchPos.toString());

			if (carInput.updated) {

				isThrottling = true;

				// compute steer angle first
				carInput.steerAngle = transformSteerAngle((float)Math.atan2(touchPos.x - carPos.x, touchPos.y - carPos.y));

				// normalize positions and compute final throttle
				touchPos.x *= invWidth;
				touchPos.y *= invHeight;
				carPos.x *= invWidth;
				carPos.y *= invHeight;
				VMath.clamp(touchPos, 0, 1);
				VMath.clamp(carPos, 0, 1);

				carInput.throttle = touchPos.dst(carPos) * 4 * preset.model.max_force;
			}

		} else {

			// keyboard input

			boolean kUp = input.isOn(Keys.UP);
			boolean kLeft = input.isOn(Keys.LEFT);
			boolean kRight = input.isOn(Keys.RIGHT);
			boolean kDown = input.isOn(Keys.DOWN);

			float maxsecs = 0.1f + AMath.fixup(1 * speed);
			float dirtimeLeft = updateDirTime(Keys.LEFT, maxsecs);
			float dirtimeRight = updateDirTime(Keys.RIGHT, maxsecs);
			// float dirtimeUp = updateDirTime(Keys.UP, maxsecs);

			carInput.updated = false;

			float KeyboardSensitivity = 2f + 5 * speed;

			if (kUp) {
				isThrottling = true;
				carInput.updated = true;
				carInput.throttle = preset.model.max_force;
			} else {
				carInput.throttle = 0;
			}

			if (AMath.isZero(carInput.throttle)) {
				if (Math.abs(carDesc.velocity_wc.x) > 0.5f || Math.abs(carDesc.velocity_wc.y) > 0.5f) {
					carInput.brake = 200;
				}
			}

			if (kDown) {
				carInput.brake += preset.model.max_force * 0.75f;
			}

			float inertialThrust = 0;

			if (kLeft) {
				carInput.updated = true;
				if (carInput.steerAngle > 0) {
					carInput.steerAngle = 0;
				}

				inertialThrust = KeyboardSensitivity * dirtimeLeft;
				carInput.steerAngle -= inertialThrust;
			} else if (kRight) {
				carInput.updated = true;
				if (carInput.steerAngle < 0) {
					carInput.steerAngle = 0;
				}

				inertialThrust = KeyboardSensitivity * dirtimeRight;
				carInput.steerAngle += inertialThrust;
			} else {
				carInput.steerAngle = 0f;
			}

			// carInput.steerAngle *= MathUtils.clamp(AMath.damping(0.7f * speed), 0, 1); // GameplaySettings.DampingKeyboardKeys;
			carInput.steerAngle *= MathUtils.clamp(AMath.damping(0.1f + 0.3f * speed), 0, 1);
			carInput.steerAngle = AMath.fixup(carInput.steerAngle);
			// Gdx.app.log("PlayerCar", "speed=" + speed);
		}

		return carInput;
	}

	private float updateDirTime (int uplr, float maxSeconds) {
		boolean isOn = input.isOn(uplr);
		float ret = 0;
		Timer t = keytimer.get(uplr);
		if (isOn) {
			t.update();
			ret = t.elapsed();
		} else {
			t.reset();
		}

		if (maxSeconds > 0) {
			ret /= maxSeconds;
		} else {
			return 0;
		}

		ret = MathUtils.clamp(ret, 0, 1);

		return ret;
	}

	private float transformSteerAngle (float angle) {
		float transformed = angle;

		transformed -= AMath.PI;
		transformed += -body.getAngle(); // to local

		if (transformed < 0) {
			transformed += AMath.TWO_PI;
		}

		transformed = -(transformed - AMath.TWO_PI);

		if (transformed > AMath.PI) {
			transformed = transformed - AMath.TWO_PI;
		}

		return transformed;// * 0.75f;
	}

	@Override
	protected void onComputeCarForces (CarForces forces) {
		carInput = acquireInput();

		handleImpactFeedback();

		carSim.applyInput(carInput);
		carSim.step(Config.Physics.Dt, body.getAngle());

		// update computed forces
		forces.velocity_x = carDesc.velocity_wc.x;
		forces.velocity_y = carDesc.velocity_wc.y;
		forces.angularVelocity = carDesc.angularvelocity;
	}

	@Override
	public void onSubstepCompleted () {
		carState.update(carDesc);
		driftState.update(carSim.lateralForceFront.y, carSim.lateralForceRear.y, carDesc.velocity_wc.len());

		if (Config.Debug.ApplyCarFrictionFromMap) {
			updateCarFriction();
			handleFriction();
		}
	}

	private Pixmap frictionMap = null;

	public void setFrictionMap (Pixmap map) {
		frictionMap = map;
	}

	private Vector2 offset = new Vector2();

	private void updateCarFriction () {
		Vector2 tilePosition = carState.tilePosition;

		if (frictionMap != null && gameWorld.isValidTilePosition(tilePosition)) {
			// compute realsize-based pixel offset car-tile (top-left origin)
			float scaledTileSize = gameWorld.getTileSizePx();
			float tsx = tilePosition.x * scaledTileSize;
			float tsy = tilePosition.y * scaledTileSize;
			offset.set(Convert.mt2px(getBody().getPosition()));
			offset.y = gameWorld.worldSizePx.y - offset.y;
			offset.x = offset.x - tsx;
			offset.y = offset.y - tsy;
			offset.scl(gameWorld.getTileSizePxInv()).scl(gameWorld.tileWidth);

			TiledMapTileLayer layerTrack = gameWorld.getLayer(Layer.Track);

			// int id = layerTrack.getCell((int)tilePosition.x, gameWorld.mapHeight - (int)tilePosition.y - 1).getTile().getId() - 1;
			int id = layerTrack.getCell((int)tilePosition.x, (int)tilePosition.y).getTile().getId() - 1;

			// int xOnMap = (id %4) * (int)gameWorld.map.tileWidth + (int)offset.x;
			// int yOnMap = (int)( id/4f ) * (int)gameWorld.map.tileWidth + (int)offset.y;

			// bit twiddling, faster version
			int xOnMap = (id & 3) * (int)gameWorld.tileWidth + (int)offset.x;
			int yOnMap = (id >> 2) * (int)gameWorld.tileWidth + (int)offset.y;

			int pixel = frictionMap.getPixel(xOnMap, yOnMap);
			boolean inTrack = (pixel == -256);
			frictionMean.addValue((inTrack ? 0 : -1));

			// Gdx.app.log( "PlayerCar", "xmap=" + xOnMap + ", ymap=" + yOnMap );
			// Gdx.app.log( "PlayerCar", "mean=" + frictionMean.getMean() + ", pixel=" + pixel + ", xmap=" + xOnMap +
			// ", ymap=" + yOnMap );
			// Gdx.app.log("PlayerCar", "id=" + id);
			// Gdx.app.log("PlayerCar", "#" + id + ", xt=" + (int)tilePosition.x + "," + (int)tilePosition.y);

		} else {
			Gdx.app.log("PlayerCar", "PlayerCar out of map!");
		}
	}

	private boolean notifiedOutOfTrack = false;
	private boolean isOutOfTrack = false;

	private void handleFriction () {
		isOutOfTrack = frictionMean.getMean() < -0.3;

		if (isOutOfTrack && !notifiedOutOfTrack) {
			notifiedOutOfTrack = true;
			GameEvents.playerCar.trigger(this, CarEvent.Type.onOutOfTrack);
		} else if (!isOutOfTrack && notifiedOutOfTrack) {
			GameEvents.playerCar.trigger(this, CarEvent.Type.onBackInTrack);
			notifiedOutOfTrack = false;
		}

		// FIXME, move these hard-coded values out of here
		if (isOutOfTrack && carDesc.velocity_wc.len2() > 10) {
			carDesc.velocity_wc.scl(GameplaySettings.DampingFriction);
		}
	}

	private void handleImpactFeedback () {
		// process impact feedback
		while (impacts > 0) {
			impacts--;

			// feed back the result to the car simulator
			carDesc.velocity_wc.set(body.getLinearVelocity());
			carDesc.angularvelocity = -body.getAngularVelocity();
		}
	}
}
