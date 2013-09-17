
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.collisions.CollisionFilters;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.GameTrack.TrackState;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.CarStillModel;
import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BodyEditorLoader;

public abstract strictfp class Car extends Box2DEntity {
	public enum InputMode {
		NoInput, InputFromPlayer, InputFromReplay
	}

	private boolean triggerEvents = false;
	protected static final Order ShadowsDrawingOrder = Order.MINUS_2;

	protected GameWorld gameWorld;
	protected GameTrack gameTrack;
	// protected CarType carType;
	protected CarStillModel stillModel;
	private TrackState trackState;

	protected int impacts = 0;

	private CarForces carForces = new CarForces();

	// distance
	private Vector2 previousPosition = new Vector2();
	private Vector2 distmp = new Vector2();

	// the car's traveled distance so far, in meters
	private float carTraveledDistance = 0;
	private int accuDistCount = 0;

	// the car's instant speed, in meters/sec
	private float carInstantSpeedMtSec = 0;

	protected CarPreset preset;
	protected InputMode inputMode = InputMode.NoInput;

	public Car (GameWorld gameWorld, GameLogic gameLogic, CarType carType, InputMode inputMode, CarPreset.Type presetType,
		boolean triggerEvents) {
		super(gameWorld.getBox2DWorld());
		this.preset = new CarPreset(presetType);
		// this.carType = carType;
		this.triggerEvents = triggerEvents;
		this.trackState = new TrackState();

		this.gameWorld = gameWorld;
		this.gameTrack = gameWorld.getGameTrack();

		stillModel = ModelFactory.createCarStillModel(gameLogic, this, presetType);
		impacts = 0;
		this.inputMode = inputMode;
		carTraveledDistance = 0;
		accuDistCount = 0;

		applyCarPhysics(carType, preset.model);

		// Gdx.app.log(getClass().getSimpleName(), "Input mode is " + inputMode.toString());
		// Gdx.app.log(getClass().getSimpleName(), "CarModel is " + preset.model.presetType.toString());
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.playerCar.removeAllListeners();
	}

	public TrackState getTrackState () {
		return trackState;
	}

	public CarStillModel getStillModel () {
		return stillModel;
	}

	public GameTrack getGameTrack () {
		return gameTrack;
	}

	public float getSteerAngleRads () {
		return 0;
	}

	private void applyCarPhysics (CarType carType, CarModel carModel) {
		if (body != null) {
			this.box2dWorld.destroyBody(body);
		}

		// body
		BodyDef bd = new BodyDef();
		bd.angle = 0;
		bd.type = BodyType.DynamicBody;
		// bd.bullet = true;

		body = box2dWorld.createBody(bd);
		body.setBullet(true);
		body.setUserData(this);

		// set physical properties and apply shape
		FixtureDef fd = new FixtureDef();
		fd.density = carModel.density;
		fd.friction = carModel.friction;
		fd.restitution = carModel.restitution;

		fd.filter.groupIndex = (short)((carType == CarType.PlayerCar) ? CollisionFilters.GroupPlayer : CollisionFilters.GroupReplay);
		fd.filter.categoryBits = (short)((carType == CarType.PlayerCar) ? CollisionFilters.CategoryPlayer
			: CollisionFilters.CategoryReplay);
		fd.filter.maskBits = (short)((carType == CarType.PlayerCar) ? CollisionFilters.MaskPlayer : CollisionFilters.MaskReplay);

		if (Config.Debug.TraverseWalls) {
			fd.filter.groupIndex = CollisionFilters.GroupNoCollisions;
		}

		BodyEditorLoader loader = new BodyEditorLoader(Gdx.files.internal("data/cars/car-shapes"));

		// WARNING! Be sure to set a value and use it then, every time this changes replays will NOT be compatible!

		// electron is made for a model2 car, w=2.5, h=3.5, h/w=1.4, w:h=1:1.4
		float scaleX = carModel.width / 2.5f;
		float scaleY = carModel.length / 3.5f;

		// the scaling factor should be 2, but in night mode is cool to see light bleeding across the edges of
		// the car, fading away as soon as the physical body is reached
		loader.attachFixture(body, "uracer-car", fd, 1.85f, scaleX, scaleY);
		Array<Fixture> fs = body.getFixtureList();
		for (Fixture f : fs) {
			f.setUserData(carType);
		}

		MassData mdata = body.getMassData();
		mdata.center.set(0, 0);
		body.setMassData(mdata);
	}

	/** A subclass will be requested to compute physical forces and impulses to be sent via this data structure: a Player subclass
	 * may compute them via its own car simulation, a Replay subclass, intead, may feed the forces directly without computing them.
	 * 
	 * @param forces computed forces shall be returned by filling the passed data structure. */
	protected abstract void onComputeCarForces (CarForces forces);

	// public CarPreset.Type getPresetType () {
	// return preset.type;
	// }

	// public CarPreset getCarPreset () {
	// return preset;
	// }

	public CarPreset getCarPreset () {
		return preset;
	}

	public CarModel getCarModel () {
		return preset.model;
	}

	public InputMode getInputMode () {
		return inputMode;
	}

	// public void setPreset (CarPreset.Type presetType) {
	// if (preset.type != presetType) {
	// preset.setTo(presetType);
	// applyCarPhysics(carType, preset.model);
	// // renderer.setAspect(preset.model, preset.type);
	// Gdx.app.log(this.getClass().getSimpleName(), "Switched to car model \"" + preset.model.presetType.toString() + "\"");
	// } else {
	// Gdx.app.log(this.getClass().getSimpleName(), "Preset unchanged, not switching to same type \"" + preset.type.toString()
	// + "\"");
	// }
	// }

	/** Returns the traveled distance, in meters, so far. Calling reset() will also reset the traveled distance. */
	public float getTraveledDistance () {
		return carTraveledDistance;
	}

	/** Returns the instant speed, in meters/s */
	public float getInstantSpeed () {
		return carInstantSpeedMtSec;
	}

	public int getAccuDistCount () {
		return accuDistCount;
	}

	public void setActive (boolean active) {
		if (active != body.isActive()) {
			body.setActive(active);
		}
	}

	public boolean isActive () {
		return body.isActive();
	}

	public void resetDistanceAndSpeed (boolean resetDistance, boolean resetSpeed) {
		if (resetDistance) {
			carTraveledDistance = 0;
			accuDistCount = 0;
		}

		if (resetSpeed) {
			carInstantSpeedMtSec = 0;
		}
	}

	public void resetPhysics () {
		body.setAngularVelocity(0);
		body.setLinearVelocity(0, 0);
		impacts = 0;
	}

	public void onCollide (Fixture other, Vector2 normalImpulses, float frontRatio) {
		impacts++;

		// FIXME
		// see the bug report at https://code.google.com/p/libgdx/issues/detail?id=1398
		if (triggerEvents) {
			GameEvents.playerCar.data.setCollisionData(other, normalImpulses, frontRatio);
			GameEvents.playerCar.trigger(this, CarEvent.Type.onCollision);
		}
	}

	@Override
	public void setWorldPosMt (Vector2 worldPosition) {
		super.setWorldPosMt(worldPosition);
		previousPosition.set(body.getPosition());
	}

	@Override
	public void setWorldPosMt (Vector2 worldPosition, float orientationRads) {
		super.setWorldPosMt(worldPosition, orientationRads);
		previousPosition.set(body.getPosition());
	}

	@Override
	public void onBeforePhysicsSubstep () {
		super.onBeforePhysicsSubstep();

		// let's subclasses behave as needed, ask them to fill carForces with new data
		onComputeCarForces(carForces);

		// trigger event, new forces have been computed
		if (triggerEvents) {
			GameEvents.playerCar.data.setForces(carForces);
			GameEvents.playerCar.trigger(this, CarEvent.Type.onPhysicsForcesReady);
		}

		// put newly computed forces into the system
		body.setLinearVelocity(carForces.velocity_x, carForces.velocity_y);
		body.setAngularVelocity(-carForces.angularVelocity);
	}

	@Override
	public void onAfterPhysicsSubstep () {
		super.onAfterPhysicsSubstep();
		computeDistanceAndSpeed();
	}

	private void computeDistanceAndSpeed () {
		// compute traveled distance, in meters
		distmp.set(body.getPosition());
		distmp.sub(previousPosition);
		previousPosition.set(body.getPosition());

		// filter out zero distance
		float dist = AMath.fixup(distmp.len());

		if (!AMath.isZero(dist)) {
			// accumulate distance
			carTraveledDistance += dist;
			accuDistCount++;
		} else {
			dist = 0;
		}

		// compute instant speed
		carInstantSpeedMtSec = AMath.fixup(dist * Config.Physics.TimestepHz);
	}
}
