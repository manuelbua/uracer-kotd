package com.bitfire.uracer.events;

public abstract class Event {
	public Object source;

	public Event(){
		this.source = null;
	}

	public Event(Object source) {
		this.source = source;
	}
}
