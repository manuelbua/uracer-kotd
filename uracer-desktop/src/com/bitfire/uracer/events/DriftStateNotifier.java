package com.bitfire.uracer.events;

public class DriftStateNotifier extends EventNotifier<DriftStateListener> implements DriftStateListener {
	@Override
	public void onBeginDrift() {
		for( DriftStateListener listener : listeners )
			listener.onBeginDrift();
	}

	@Override
	public void onEndDrift() {
		for( DriftStateListener listener : listeners )
			listener.onEndDrift();
	}
}
