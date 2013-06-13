
package com.bitfire.uracer.game.events;

import com.badlogic.gdx.utils.Array;

public abstract class Event<T extends Enum<T>, O extends Enum<O>, L extends Event.Listener<T, O>> {
	private Array<Listener<T, O>>[][] listeners;
	private Class<T> classType;
	private Class<O> classOrder;
	private int typeCount;
	private int orderCount;

	public interface Listener<T, O> {
		public abstract void handle (Object source, T type, O order);
	}

	@SuppressWarnings("unchecked")
	public Event (Class<T> classType, Class<O> classOrder) {
		this.classType = classType;
		this.classOrder = classOrder;
		typeCount = classType.getEnumConstants().length;
		orderCount = classOrder.getEnumConstants().length;
		listeners = new Array[typeCount][orderCount];

		for (T t : classType.getEnumConstants()) {
			for (O o : classOrder.getEnumConstants()) {
				listeners[t.ordinal()][o.ordinal()] = new Array<Listener<T, O>>(false, 4);
			}
		}
	}

	public void addListener (Listener<T, O> listener, T type) {
		addListener(listener, type, classOrder.getEnumConstants()[0]);
	}

	public void removeListener (Listener<T, O> listener, T type) {
		removeListener(listener, type, classOrder.getEnumConstants()[0]);
	}

	public void addListener (Listener<T, O> listener, T type, O order) {
		Array<Listener<T, O>> ls = listeners[type.ordinal()][order.ordinal()];
		if (!ls.contains(listener, true)) {
			ls.add(listener);
		}
	}

	public void removeListener (Listener<T, O> listener, T type, O order) {
		Array<Listener<T, O>> ls = listeners[type.ordinal()][order.ordinal()];

		int pos = ls.indexOf(listener, true);
		if (pos > -1) {
			ls.removeIndex(pos);
		}
	}

	public void removeAllListeners () {
		for (T t : classType.getEnumConstants()) {
			for (O o : classOrder.getEnumConstants()) {
				listeners[t.ordinal()][o.ordinal()].clear();
			}
		}
	}

	public void trigger (Object source, T type) {
		for (O o : classOrder.getEnumConstants()) {
			for (Listener<T, O> listener : listeners[type.ordinal()][o.ordinal()]) {
				listener.handle(source, type, o);
			}
		}
	}

	public void trigger (Object source, T type, O order) {
		for (Listener<T, O> listener : listeners[type.ordinal()][order.ordinal()]) {
			listener.handle(source, type, order);
		}
	}
}
