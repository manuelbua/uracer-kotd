package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Entity
{
	protected EntityScreenState stateRender = new EntityScreenState();

	public void onTick( float dt )
	{}

	public void onRender( SpriteBatch batch, Camera screen, Camera world, float temporalAliasingFactor )
	{}

	public EntityScreenState getState()
	{
		return stateRender;
	}
}
