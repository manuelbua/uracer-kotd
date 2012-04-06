package com.bitfire.uracer.entities;


public abstract class Entity {
	// screen-coords
	protected EntityState stateRender = new EntityState();

//	public void onTick() {
//	}

//	public void onRender( SpriteBatch batch ) {
//	}

	public void onDebug() {
	}

	public EntityState state() {
		return stateRender;
	}
}
