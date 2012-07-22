package com.bitfire.uracer.game.logic.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.VMath;

public class CameraController {
	public enum InterpolationMode {
		OffNoBounds, Off, Linear, Sigmoid, Gentle
	}

	private float boundsWidth = 0, boundsHeight = 0;
	private PositionInterpolator interpolator;
	private float sigmoidStrengthX = 1f;
	private float sigmoidStrengthY = 1f;
	private Vector2 worldTiles = new Vector2();

	public CameraController( InterpolationMode mode, Vector2 halfViewport, final Vector2 worldSizeScaledPx, Vector2 worldSizeTiles ) {
		final Rectangle cameraBounds = new Rectangle();
		cameraBounds.x = halfViewport.x;
		cameraBounds.width = worldSizeScaledPx.x - halfViewport.x;
		cameraBounds.height = halfViewport.y;
		cameraBounds.y = worldSizeScaledPx.y - halfViewport.y;

		boundsWidth = cameraBounds.width - cameraBounds.x;
		boundsHeight = cameraBounds.y - cameraBounds.height;

		worldTiles.set( worldSizeTiles );

		sigmoidStrengthX = AMath.clamp( (worldSizeTiles.x / 4f), 1f, 5f );
		sigmoidStrengthY = AMath.clamp( (worldSizeTiles.y / 4f), 1f, 5f );

		switch( mode ) {
		case Linear:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 target, float orient ) {
					// [0,1]
					float x_ratio = target.x / worldSizeScaledPx.x;
					float y_ratio = target.y / worldSizeScaledPx.y;

					tmp.x = cameraBounds.x + x_ratio * boundsWidth;
					tmp.y = cameraBounds.height + y_ratio * boundsHeight;

					return tmp;
				}
			};
			break;

		case Sigmoid:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 target, float orient ) {
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
		case Gentle:
			interpolator = new PositionInterpolator() {
				private float amount = 150f;
				private float speed = 1f;

				@Override
				public void setInitialPositionOrientation( Vector2 position, float orient ) {
					float x_ratio = position.x / (worldSizeScaledPx.x);
					float y_ratio = position.y / (worldSizeScaledPx.y);
					pheading.set( VMath.fromDegrees( orient ) );
					tmp.x = cameraBounds.x + x_ratio * (boundsWidth) + pheading.x * amount;
					tmp.y = cameraBounds.height + y_ratio * (boundsHeight) - pheading.y * amount;
					prev.set( tmp );
				}

				@Override
				public Vector2 transform( Vector2 target, float orient ) {
					float dt = Gdx.graphics.getDeltaTime();

					heading.set( VMath.fromDegrees( orient ) );
					heading.x = AMath.lerp( pheading.x, heading.x, dt * speed );
					heading.y = AMath.lerp( pheading.y, heading.y, dt * speed );
					pheading.set( heading );

					// [0,1]
					float x_ratio = target.x / (worldSizeScaledPx.x);
					float y_ratio = target.y / (worldSizeScaledPx.y);

					tmp.x = cameraBounds.x + x_ratio * (boundsWidth) + heading.x * amount;
					tmp.y = cameraBounds.height + y_ratio * (boundsHeight) - heading.y * amount;

					tmp.x = AMath.lerp( prev.x, tmp.x, 0.1f );
					tmp.y = AMath.lerp( prev.y, tmp.y, 0.1f );

					prev.set( tmp );

					return tmp;
				}
			};
			break;
		case Off:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 target, float orient ) {
					tmp.set( target );

					if( tmp.x < cameraBounds.x ) {
						tmp.x = cameraBounds.x;
					}
					if( tmp.x > cameraBounds.width ) {
						tmp.x = cameraBounds.width;
					}
					if( tmp.y > cameraBounds.y ) {
						tmp.y = cameraBounds.y;
					}
					if( tmp.y < cameraBounds.height ) {
						tmp.y = cameraBounds.height;
					}

					return tmp;
				}
			};
			break;
		default:
		case OffNoBounds:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform( Vector2 targetPosition, float orient ) {
					return targetPosition;
				}
			};
			break;
		}
	}

	public Vector2 transform( Vector2 position, float orient ) {
		return interpolator.transform( position, orient );
	}

	public void setInitialPositionOrient( Vector2 position, float orient ) {
		interpolator.setInitialPositionOrientation( position, orient );
	}

	private abstract class PositionInterpolator {
		protected Vector2 tmp = new Vector2();
		protected Vector2 prev = new Vector2();
		protected Vector2 heading = new Vector2();
		protected Vector2 pheading = new Vector2();

		public PositionInterpolator() {
		}

		public void setInitialPositionOrientation( Vector2 position, float orient ) {
		}

		public abstract Vector2 transform( Vector2 targetPosition, float orient );
	}
}
