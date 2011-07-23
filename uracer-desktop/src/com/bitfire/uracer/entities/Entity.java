package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Entity
{
	protected EntityScreenState stateRender = new EntityScreenState();

	public void onTick()
	{
	}

	public void onRender( SpriteBatch batch, float temporalAliasingFactor )
	{
	}

	public EntityScreenState getState()
	{
		return stateRender;
	}
}
