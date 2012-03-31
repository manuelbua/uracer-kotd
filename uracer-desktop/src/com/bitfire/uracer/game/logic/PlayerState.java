package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.events.PlayerStateListener;
import com.bitfire.uracer.events.PlayerStateNotifier;
import com.bitfire.uracer.utils.AMath;

public class PlayerState {
	public final Car car;
	public final GhostCar ghost;

	public float carMaxSpeedSquared = 0;
	public float carMaxForce = 0;
	public float currCarSpeedSquared = 0;
	public float currSpeedFactor = 0;
	public float currForceFactor = 0;

	/* position/orientation */

	// initial
	public final Vector2 startPos = new Vector2();
	public int startTileX = 1, startTileY = 1;
	public float startOrient = 0f;

	// current
	public int currTileX = 1, currTileY = 1;

	private PlayerStateNotifier notifier;

	public PlayerState( Car car, GhostCar ghost ) {
		this.car = car;
		this.ghost = ghost;
		this.notifier = new PlayerStateNotifier();

		// precompute factors
		carMaxSpeedSquared = car.getCarDescriptor().carModel.max_speed * car.getCarDescriptor().carModel.max_speed;
		carMaxForce = car.getCarDescriptor().carModel.max_force;
	}

	private int lastTileX = 0, lastTileY = 0;

	public void addListener(PlayerStateListener listener) {
		notifier.addListener( listener );
	}

	public void tick() {
		// onTileChanged
		lastTileX = currTileX;
		lastTileY = currTileY;
		Vector2 tp = car.getTilePosition();
		currTileX = (int)tp.x;
		currTileY = (int)tp.y;
		if( (lastTileX != currTileX) || (lastTileY != currTileY) ) {
			notifier.onTileChanged();
		}

		// speed/force normalized factors
		currCarSpeedSquared = car.getCarDescriptor().velocity_wc.len2();
		currSpeedFactor = AMath.clamp( currCarSpeedSquared / carMaxSpeedSquared, 0f, 1f );
		currForceFactor = AMath.clamp( car.getCarDescriptor().throttle / carMaxForce, 0f, 1f );
	}

	public void reset() {
		car.reset();
		ghost.reset();

		// causes an onTileChanged event to be raised
		lastTileX = lastTileY = currTileX = currTileY = -1;
	}
}
