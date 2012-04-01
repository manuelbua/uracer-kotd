package com.bitfire.uracer.events;

public class PlayerStateNotifier extends EventNotifier<PlayerStateListener> implements PlayerStateListener {
	@Override
	public void onTileChanged() {
		for( PlayerStateListener listener : listeners )
			listener.onTileChanged();
	}
}
