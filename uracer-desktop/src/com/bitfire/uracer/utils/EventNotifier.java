package com.bitfire.uracer.utils;

import com.badlogic.gdx.utils.Array;

public class EventNotifier<L> {
	protected final Array<L> listeners;

	public EventNotifier() {
		listeners = new Array<L>();
	}

	public void addListener( L listener ) {
		listeners.add( listener );
	}
}
