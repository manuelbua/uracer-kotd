package com.bitfire.uracer.events;

import com.bitfire.uracer.events.GameLogicEvent.EventType;

public interface GameLogicListener extends EventListener {
	void gameLogicEvent(EventType type);
}
