
package com.bitfire.uracer.events;

import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.player.PlayerDriftState;

public final class PlayerDriftStateEvent extends Event<PlayerDriftState> {
	public enum Type {
		onBeginDrift, onEndDrift
	}

	public interface Listener extends EventListener {
		void playerDriftStateEvent (PlayerCar source, Type type);
	}

	public PlayerDriftStateEvent (PlayerDriftState playerDriftState) {
		super(playerDriftState);
		for (Type t : Type.values()) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener (Listener listener, Type type) {
		notifiers[type.ordinal()].addListener(listener);
	}

	public void removeListener (Listener listener, Type type) {
		notifiers[type.ordinal()].removeListener(listener);
	}

	public void removeAllListeners () {
		for (Type t : Type.values()) {
			notifiers[t.ordinal()].removeAllListeners();
		}
	}

	public void trigger (PlayerCar source, Type type) {
		this.source = source;
		notifiers[type.ordinal()].playerDriftStateEvent(source, type);
	}

	public PlayerCar source;
	private Notifier[] notifiers = new Notifier[Type.values().length];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void playerDriftStateEvent (PlayerCar source, Type type) {
			for (Listener listener : listeners) {
				listener.playerDriftStateEvent(source, type);
			}
		}
	};
}
