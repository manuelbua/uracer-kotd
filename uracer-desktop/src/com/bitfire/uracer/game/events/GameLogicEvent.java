
package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.player.PlayerCar;

public class GameLogicEvent extends Event<GameLogicEvent.Type, GameLogicEvent.Order, GameLogicEvent.Listener> {
	/** defines the type of render queue */
	public enum Type {
		//@off
		PlayerAdded,
		PlayerRemoved,
		GameRestart,
		GameReset,
		GameQuit
		;
		//@on
	}

	/** defines the position in the render queue specified by the Type parameter */
	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public PlayerCar player;

	public GameLogicEvent () {
		super(Type.class, Order.class);
	}
}
