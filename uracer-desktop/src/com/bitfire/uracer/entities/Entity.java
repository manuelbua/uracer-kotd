package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Entity
{
	// screen-coords
	protected EntityState stateRender = new EntityState();

	public void onTick()
	{
	}

	public void onRender( SpriteBatch batch )
	{
	}

	public EntityState getState()
	{
		return stateRender;
	}
}
