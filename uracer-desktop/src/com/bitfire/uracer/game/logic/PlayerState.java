package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.entities.Car;
import com.bitfire.uracer.game.entities.GhostCar;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.PlayerStateEvent;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.AMath;

public final class PlayerState extends Task {
	public Car car;
	public GhostCar ghost;

	public float carMaxSpeedSquared = 0;
	public float carMaxForce = 0;
	public float currCarSpeedSquared = 0;
	public float currSpeedFactor = 0;
	public float currForceFactor = 0;

	/* position/orientation */

	public int currTileX = 1, currTileY = 1;
	private int lastTileX = 0, lastTileY = 0;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( com.bitfire.uracer.game.events.GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				reset();
				break;
			}
		}
	};

	public PlayerState() {
		this( null, null );
	}

	public PlayerState( Car car, GhostCar ghost ) {
		Events.gameLogic.addListener( gameLogicEvent );

		Events.playerState.source = this;
		setData( car, ghost );
	}

	public void setData( Car car, GhostCar ghost ) {
		this.car = car;
		this.ghost = ghost;

		// precompute factors
		if( car != null ) {
			carMaxSpeedSquared = car.getCarDescriptor().carModel.max_speed * car.getCarDescriptor().carModel.max_speed;
			carMaxForce = car.getCarDescriptor().carModel.max_force;
		}
	}

	@Override
	protected void onTick() {
		// onTileChanged
		lastTileX = currTileX;
		lastTileY = currTileY;
		Vector2 tp = car.getTilePosition();
		currTileX = (int)tp.x;
		currTileY = (int)tp.y;

		if( (lastTileX != currTileX) || (lastTileY != currTileY) ) {
			Events.playerState.trigger( PlayerStateEvent.Type.onTileChanged );
		}

		if( car != null ) {
			// speed/force normalized factors
			currCarSpeedSquared = car.getCarDescriptor().velocity_wc.len2();
			currSpeedFactor = AMath.clamp( currCarSpeedSquared / carMaxSpeedSquared, 0f, 1f );
			currForceFactor = AMath.clamp( car.getCarDescriptor().throttle / carMaxForce, 0f, 1f );
		}
	}

	public void reset() {
		if( car != null ) {
			car.reset();
			car.setTransform( GameData.gameWorld.playerStartPos, GameData.gameWorld.playerStartOrient );
		}

		if( ghost != null ) {
			ghost.reset();
		}

		// causes an onTileChanged event to be raised
		lastTileX = -1;
		lastTileY = -1;
		currTileX = -1;
		currTileY = -1;
	}
}
