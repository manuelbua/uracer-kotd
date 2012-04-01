package com.bitfire.uracer.events;

public class GameLogicNotifier extends EventNotifier<GameLogicListener> implements GameLogicListener {
	@Override
	public void onReset() {
		for( GameLogicListener listener : listeners )
			listener.onReset();
	}

	@Override
	public void onRestart() {
		for( GameLogicListener listener : listeners )
			listener.onRestart();
	}
}
