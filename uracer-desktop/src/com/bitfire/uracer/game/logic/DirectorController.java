package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.utils.AMath;

public class DirectorController {
	public enum InterpolationMode {
		Off, Linear, Sigmoid
	}

	private float boundsWidth = 0, boundsHeight = 0;
	private PositionInterpolator interpolator;
	private float sigmoidStrengthX = 1f;
	private float sigmoidStrengthY = 1f;

	public DirectorController( InterpolationMode mode, final Rectangle bounds ) {
		boundsWidth = bounds.width - bounds.x;
		boundsHeight = bounds.y - bounds.height;

		sigmoidStrengthX = AMath.clamp( (Director.worldSizeTiles.x / 4f), 1f, 5f );
		sigmoidStrengthY = AMath.clamp( (Director.worldSizeTiles.y / 4f), 1f, 5f );

		switch( mode ) {
		default:
		case Off:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 targetPosition ) {
					return targetPosition;
				}
			};
			break;
		case Linear:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 targetPosition ) {
					// [0,1]
					float x_ratio = targetPosition.x / Director.worldSizeScaledPx.x;
					float y_ratio = targetPosition.y / Director.worldSizeScaledPx.y;

					tmp.x = bounds.x + x_ratio * boundsWidth;
					tmp.y = bounds.height + y_ratio * boundsHeight;

					return tmp;
				}
			};
			break;

		case Sigmoid:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 target ) {
					float tx = target.x;
					float ty = target.y;

					// [-1, 1]
					float x_ratio = ((tx / Director.worldSizeScaledPx.x) - 0.5f) * 2;
					float y_ratio = ((ty / Director.worldSizeScaledPx.y) - 0.5f) * 2;

					tmp.x = Director.boundsPx.x + AMath.sigmoid( x_ratio * sigmoidStrengthX ) * boundsWidth;
					tmp.y = Director.boundsPx.height + AMath.sigmoid( y_ratio * sigmoidStrengthY ) * boundsHeight;

					return tmp;
				}
			};
			break;
		}
	}

	public void setPosition( Vector2 pos ) {
		Director.setPositionPx( interpolator.transform( pos ), false, true );
	}

	private abstract class PositionInterpolator {
		protected Vector2 tmp = new Vector2();

		public PositionInterpolator() {
		}

		public abstract Vector2 transform( Vector2 targetPosition );
	}
}
