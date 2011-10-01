package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;

public class DirectorController
{
	public enum FollowMode
	{
		Linear, Sigmoid
	}

	private float boundsWidth = 0, boundsHeight = 0;
	private PositionInterpolator interpolator;

	public DirectorController( FollowMode mode )
	{
		boundsWidth = Director.boundsPx.width - Director.boundsPx.x;
		boundsHeight = Director.boundsPx.y - Director.boundsPx.height;

		switch( mode )
		{
		default:
		case Linear:
			interpolator = new PositionInterpolator()
			{
				@Override
				public Vector2 transform( Vector2 targetPosition )
				{
					// [0,1]
					float x_ratio = targetPosition.x / Director.worldSizeScaledPx.x;
					float y_ratio = targetPosition.y / Director.worldSizeScaledPx.y;

					tmp.x = Director.boundsPx.x + x_ratio * boundsWidth;
					tmp.y = Director.boundsPx.height + y_ratio * boundsHeight;

					return tmp;
				}
			};
			break;

		case Sigmoid:
			interpolator = new PositionInterpolator()
			{
				private float sigmoid( float strength )
				{
					return (float)(1f / (1f + Math.pow( Math.E, -strength )));
				}

				@Override
				public Vector2 transform( Vector2 target )
				{
					float tx = target.x;
					float ty = target.y;

					// [-1, 1]
					float x_ratio = ((tx / Director.worldSizeScaledPx.x) - 0.5f) * 2;
					float y_ratio = ((ty / Director.worldSizeScaledPx.y) - 0.5f) * 2;

					float strength = 5f;
					tmp.x = Director.boundsPx.x + sigmoid( x_ratio * strength ) * boundsWidth;
					tmp.y = Director.boundsPx.height + sigmoid( y_ratio * strength ) * boundsHeight;

					return tmp;
				}
			};
			break;
		}
	}

	public void tick()
	{
	}

	public void setPosition( Vector2 pos )
	{
		Director.setPositionPx( interpolator.transform(pos), false, true );
	}

	private abstract class PositionInterpolator
	{
		protected Vector2 tmp = new Vector2();

		public PositionInterpolator()
		{
		}

		public abstract Vector2 transform( Vector2 targetPosition );
	}
}
