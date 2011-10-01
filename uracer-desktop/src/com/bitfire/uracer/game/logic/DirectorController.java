package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;

public class DirectorController
{
	private float boundsWidth = 0, boundsHeight = 0;

	public DirectorController()
	{
		boundsWidth = Director.boundsPx.width - Director.boundsPx.x;
		boundsHeight = Director.boundsPx.y - Director.boundsPx.height;
	}

	public void tick()
	{
	}

	private Vector2 tmp = new Vector2();

	protected Vector2 linear( Vector2 target )
	{
		float x_ratio = target.x / Director.worldSizeScaledPx.x;
		float y_ratio = target.y / Director.worldSizeScaledPx.y;

		tmp.x = Director.boundsPx.x + x_ratio * boundsWidth;
		tmp.y = Director.boundsPx.height + y_ratio * boundsHeight;

		return tmp;
	}

	protected float sigmoid( float strength )
	{
		return (float)(1f / (1f + Math.pow( Math.E, -strength )));
	}

	protected Vector2 soft( Vector2 target )
	{
		float tx = target.x;
		float ty = target.y;

		float x_ratio = ((tx / Director.worldSizeScaledPx.x) - 0.5f) * 2;
		float y_ratio = ((ty / Director.worldSizeScaledPx.y) - 0.5f) * 2;

		// System.out.println("x_ration=" + x_ratio);

		float strength = 5f;
		tmp.x = Director.boundsPx.x + sigmoid( x_ratio * strength ) * boundsWidth;
		tmp.y = Director.boundsPx.height + sigmoid( y_ratio * strength ) * boundsHeight;

		return tmp;
	}

	public void setPosition( Vector2 pos )
	{
		Director.setPositionPx( soft( pos ), false, true );
	}
}
