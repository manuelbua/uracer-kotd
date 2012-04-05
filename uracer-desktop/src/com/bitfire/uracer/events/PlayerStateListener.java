package com.bitfire.uracer.events;

import com.bitfire.uracer.events.PlayerStateEvent.EventType;

public interface PlayerStateListener extends EventListener {
	void playerStateEvent( EventType type );
}
