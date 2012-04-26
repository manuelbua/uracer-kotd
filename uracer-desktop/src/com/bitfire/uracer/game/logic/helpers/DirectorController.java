package com.bitfire.uracer.game.logic.helpers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.Director;
import com.bitfire.uracer.utils.AMath;

public class DirectorController {
	public enum InterpolationMode {
		Off, Linear, Sigmoid
	}

	private float boundsWidth = 0, boundsHeight = 0;
	private PositionInterpolator interpolator;
	private float sigmoidStrengthX = 1f;
	private float sigmoidStrengthY = 1f;

	public DirectorController( InterpolationMode mode, Vector2 halfViewport, final Vector2 worldSizeScaledPx, Vector2 worldSizeTiles ) {
		final Rectangle cameraBounds = new Rectangle();
		cameraBounds.x = halfViewport.x;
		cameraBounds.width = worldSizeScaledPx.x - halfViewport.x;
		cameraBounds.height = halfViewport.y;
		cameraBounds.y = worldSizeScaledPx.y - halfViewport.y;

		boundsWidth = cameraBounds.width - cameraBounds.x;
		boundsHeight = cameraBounds.y - cameraBounds.height;

		sigmoidStrengthX = AMath.clamp( (worldSizeTiles.x / 4f), 1f, 5f );
		sigmoidStrengthY = AMath.clamp( (worldSizeTiles.y / 4f), 1f, 5f );

		switch( mode ) {
		case Linear:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 targetPosition ) {
					// [0,1]
					float x_ratio = targetPosition.x / worldSizeScaledPx.x;
					float y_ratio = targetPosition.y / worldSizeScaledPx.y;

					tmp.x = cameraBounds.x + x_ratio * boundsWidth;
					tmp.y = cameraBounds.height + y_ratio * boundsHeight;

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
					float x_ratio = ((tx / worldSizeScaledPx.x) - 0.5f) * 2;
					float y_ratio = ((ty / worldSizeScaledPx.y) - 0.5f) * 2;

					tmp.x = cameraBounds.x + AMath.sigmoid( x_ratio * sigmoidStrengthX ) * boundsWidth;
					tmp.y = cameraBounds.height + AMath.sigmoid( y_ratio * sigmoidStrengthY ) * boundsHeight;

					return tmp;
				}
			};
			break;
		default:
		case Off:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 targetPosition ) {

					if( targetPosition.x < cameraBounds.x ) {
						targetPosition.x = cameraBounds.x;
					}
					if( targetPosition.x > cameraBounds.width ) {
						targetPosition.x = cameraBounds.width;
					}
					if( targetPosition.y > cameraBounds.y ) {
						targetPosition.y = cameraBounds.y;
					}
					if( targetPosition.y < cameraBounds.height ) {
						targetPosition.y = cameraBounds.height;
					}

					return targetPosition;
				}
			};
			break;
		}
	}

	public void setPosition( Vector2 pos ) {
		Director.setPositionPx( interpolator.transform( pos ), true );
	}

	private abstract class PositionInterpolator {
		protected Vector2 tmp = new Vector2();

		public PositionInterpolator() {
		}

		public abstract Vector2 transform( Vector2 targetPosition );
	}
}
