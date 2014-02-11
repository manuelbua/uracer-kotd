
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public final class CarState {
	/* observed car */
	public final Car car;

	/* position */
	public int currTileX = -1, currTileY = -1;
	public Vector2 tilePosition = new Vector2();

	/* speed/force factors */
	public float currVelocityLenSquared = 0;
	public float currThrottle = 0;
	public float currSpeedFactor = 0;
	// public float currBrakeFactor = 0;
	public float currForceFactor = 0;

	// temporaries
	private final float CarMaxSpeedSquared;
	private final float CarMaxForce;
	// private int lastTileX = 0, lastTileY = 0;

	private GameWorld world;

	public CarState (GameWorld world, Car car) {
		this.world = world;
		this.car = car;

		// precompute factors
		if (car != null) {
			CarMaxSpeedSquared = car.getCarModel().max_speed * car.getCarModel().max_speed;
			CarMaxForce = car.getCarModel().max_force;
		} else {
			CarMaxSpeedSquared = 1;
			CarMaxForce = 1;
		}
	}

	public void dispose () {
		// GameEvents.playerCarState.removeAllListeners();
	}

	public void reset () {
		// causes an onTileChanged event to be raised the next update step
		// lastTileX = -1;
		// lastTileY = -1;
		currTileX = -1;
		currTileY = -1;
	}

	public void update (CarDescriptor carDescriptor) {
		if (carDescriptor != null) {
			updateFactors(carDescriptor);
		}

		updateTilePosition();
	}

	private void updateFactors (CarDescriptor carDescriptor) {
		// speed/force normalized factors
		currVelocityLenSquared = carDescriptor.velocity_wc.len2();
		currThrottle = carDescriptor.throttle;
		currSpeedFactor = AMath.fixup(AMath.clamp(currVelocityLenSquared / CarMaxSpeedSquared, 0f, 1f));
		// currBrakeFactor = AMath.fixup(AMath.clamp(carDescriptor.brake / CarMaxForce, 0f, 1f));
		currForceFactor = AMath.fixup(AMath.clamp(currThrottle / CarMaxForce, 0f, 1f));
	}

	/*
	 * Keeps track of the car's tile position and trigger a TileChanged event whenever the car's world position translates to a
	 * tile index that is different than the previous one
	 */
	private void updateTilePosition () {
		// lastTileX = currTileX;
		// lastTileY = currTileY;

		// compute car's tile position
		tilePosition
			.set(world.pxToTile(Convert.mt2px(car.getBody().getPosition().x), Convert.mt2px(car.getBody().getPosition().y)));

		currTileX = (int)tilePosition.x;
		currTileY = (int)tilePosition.y;

		// if ((lastTileX != currTileX) || (lastTileY != currTileY)) {
		// GameEvents.playerCarState.trigger(this, CarStateEvent.Type.onTileChanged);
		// }
	}
}
