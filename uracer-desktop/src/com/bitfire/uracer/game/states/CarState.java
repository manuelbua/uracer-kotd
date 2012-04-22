package com.bitfire.uracer.game.states;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.events.CarStateEvent;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;

public final class CarState {
	public Car car;

	private float carMaxSpeedSquared = 0;
	private float carMaxForce = 0;

	private int lastTileX = 0, lastTileY = 0;
	private GameWorld world;

	/* position */
	public int currTileX = 1, currTileY = 1;
	public Vector2 tilePosition = new Vector2();

	/* speed/force factors */
	public float currCarSpeedSquared = 0;
	public float currSpeedFactor = 0;
	public float currForceFactor = 0;

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

	public CarState( GameWorld world, Car car ) {
		this.world = world;
		this.car = car;

		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );

		// precompute factors
		if( car != null ) {
			carMaxSpeedSquared = car.getCarModel().max_speed * car.getCarModel().max_speed;
			carMaxForce = car.getCarModel().max_force;
		}
	}

	public void update() {
		lastTileX = currTileX;
		lastTileY = currTileY;

		// compute car's tile position
		tilePosition.set( world.pxToTile( car.state().position.x, car.state().position.y ) );

		currTileX = (int)tilePosition.x;
		currTileY = (int)tilePosition.y;

		if( (lastTileX != currTileX) || (lastTileY != currTileY) ) {
			GameEvents.carState.trigger( car, CarStateEvent.Type.onTileChanged );
//			Gdx.app.log( "CarState", car.getClass().getSimpleName() + " onTileChanged(" + currTileX + "," + currTileY + ")" );
		}

		// speed/force normalized factors
		currCarSpeedSquared = car.getVelocity().len2();
		currSpeedFactor = AMath.clamp( currCarSpeedSquared / carMaxSpeedSquared, 0f, 1f );
		currForceFactor = AMath.clamp( car.getThrottle() / carMaxForce, 0f, 1f );
	}

	public void reset() {
		// causes an onTileChanged event to be raised the next update step
		lastTileX = -1;
		lastTileY = -1;
		currTileX = -1;
		currTileY = -1;
	}
}
