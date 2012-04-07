package com.bitfire.uracer.entities;

import com.badlogic.gdx.utils.Disposable;


public abstract class Entity implements Disposable {
	// screen-coords
	protected EntityState stateRender = new EntityState();

	public EntityState state() {
		return stateRender;
	}
}
