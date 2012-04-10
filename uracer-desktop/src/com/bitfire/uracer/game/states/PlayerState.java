package com.bitfire.uracer.game.states;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.MapUtils;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.PlayerStateEvent;
import com.bitfire.uracer.utils.AMath;

public final class PlayerState {
	public Car car;
	public GhostCar ghost;

	public float carMaxSpeedSquared = 0;
	public float carMaxForce = 0;
	public float currCarSpeedSquared = 0;
	public float currSpeedFactor = 0;
	public float currForceFactor = 0;

	/* position */
	public int currTileX = 1, currTileY = 1;
	public Vector2 tilePosition = new Vector2();
	private int lastTileX = 0, lastTileY = 0;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( GameLogicEvent.Type type ) {
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
		GameEvents.playerState.source = this;
		GameEvents.gameLogic.addListener( gameLogicEvent );
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

	public void update( MapUtils mapUtils ) {
		if( car != null ) {
			// onTileChanged
			lastTileX = currTileX;
			lastTileY = currTileY;

			// compute car's tile position
			tilePosition.set( mapUtils.pxToTile( car.state().position.x, car.state().position.y ) );

			currTileX = (int)tilePosition.x;
			currTileY = (int)tilePosition.y;

			if( (lastTileX != currTileX) || (lastTileY != currTileY) ) {
				GameEvents.playerState.trigger( PlayerStateEvent.Type.onTileChanged );
			}

			// speed/force normalized factors
			currCarSpeedSquared = car.getCarDescriptor().velocity_wc.len2();
			currSpeedFactor = AMath.clamp( currCarSpeedSquared / carMaxSpeedSquared, 0f, 1f );
			currForceFactor = AMath.clamp( car.getCarDescriptor().throttle / carMaxForce, 0f, 1f );
		}
	}

	public void reset() {
		if( car != null ) {
			car.reset();
			car.setTransform( GameData.Environment.gameWorld.playerStartPos, GameData.Environment.gameWorld.playerStartOrient );
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
