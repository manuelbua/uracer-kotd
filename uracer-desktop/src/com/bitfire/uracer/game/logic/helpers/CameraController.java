
package com.bitfire.uracer.game.logic.helpers;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.utils.AMath;

public class CameraController {
	public enum InterpolationMode {
		OffNoBounds, Off, Linear, Sigmoid
	}

	private float boundsWidth = 0, boundsHeight = 0;
	private PositionInterpolator interpolator;
	private float sigmoidStrengthX = 1f;
	private float sigmoidStrengthY = 1f;
	private Vector2 worldTiles = new Vector2();

	public CameraController (InterpolationMode mode, Vector2 halfViewport, final Vector2 worldSizeScaledPx, Vector2 worldSizeTiles) {

		final Rectangle cameraBounds = new Rectangle();

		cameraBounds.x = halfViewport.x;
		cameraBounds.width = worldSizeScaledPx.x - halfViewport.x;
		cameraBounds.height = halfViewport.y;
		cameraBounds.y = worldSizeScaledPx.y - halfViewport.y;

		boundsWidth = cameraBounds.width - cameraBounds.x;
		boundsHeight = cameraBounds.y - cameraBounds.height;

		worldTiles.set(worldSizeTiles);

		sigmoidStrengthX = AMath.clamp((worldSizeTiles.x / 4f), 1f, 5f);
		sigmoidStrengthY = AMath.clamp((worldSizeTiles.y / 4f), 1f, 5f);

		switch (mode) {
		case Linear:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform (Vector2 target, float zoom) {

					// [0,1]
					float x_ratio = target.x / worldSizeScaledPx.x;
					float y_ratio = target.y / worldSizeScaledPx.y;

					tmp.x = cameraBounds.x + x_ratio * boundsWidth;
					tmp.y = cameraBounds.height + y_ratio * boundsHeight;
					if (tmp.x < cameraBounds.x) {
						tmp.x = cameraBounds.x;
					}
					if (tmp.x > cameraBounds.width) {
						tmp.x = cameraBounds.width;
					}
					if (tmp.y > cameraBounds.y) {
						tmp.y = cameraBounds.y;
					}
					if (tmp.y < cameraBounds.height) {
						tmp.y = cameraBounds.height;
					}

					// default target
					tmp2.set(target);
					if (tmp2.x < cameraBounds.x) {
						tmp2.x = cameraBounds.x;
					}
					if (tmp2.x > cameraBounds.width) {
						tmp2.x = cameraBounds.width;
					}
					if (tmp2.y > cameraBounds.y) {
						tmp2.y = cameraBounds.y;
					}
					if (tmp2.y < cameraBounds.height) {
						tmp2.y = cameraBounds.height;
					}

					// give meaning only to the positive side [0,1,2]
					float zoomFactor = (zoom - 1) / (GameWorldRenderer.MaxCameraZoom - 1);
					tmp.lerp(tmp2, zoomFactor * 0.35f);

					// Gdx.app.log("", "" + zoom + "/" + zoomFactor);

					// tmp.x = target.x;
					// tmp.y = target.y;

					return tmp;
				}
			};
			break;

		case Sigmoid:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform (Vector2 target, float zoom) {
					float tx = target.x;
					float ty = target.y;

					// [-1, 1]
					float x_ratio = ((tx / worldSizeScaledPx.x) - 0.5f) * 2;
					float y_ratio = ((ty / worldSizeScaledPx.y) - 0.5f) * 2;

					tmp.x = cameraBounds.x + AMath.sigmoid(x_ratio * sigmoidStrengthX) * boundsWidth;
					tmp.y = cameraBounds.height + AMath.sigmoid(y_ratio * sigmoidStrengthY) * boundsHeight;

					return tmp;
				}
			};
			break;

		case Off:
			interpolator = new PositionInterpolator() {
				@Override
				public Vector2 transform (Vector2 target, float zoom) {
					tmp.set(target);

					if (tmp.x < cameraBounds.x) {
						tmp.x = cameraBounds.x;
					}
					if (tmp.x > cameraBounds.width) {
						tmp.x = cameraBounds.width;
					}
					if (tmp.y > cameraBounds.y) {
						tmp.y = cameraBounds.y;
					}
					if (tmp.y < cameraBounds.height) {
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
				public Vector2 transform (Vector2 targetPosition, float zoom) {
					return targetPosition;
				}
			};
			break;
		}
	}

	public Vector2 transform (Vector2 position, float zoom) {
		return interpolator.transform(position, zoom);
	}

	private abstract class PositionInterpolator {
		protected Vector2 tmp = new Vector2();
		protected Vector2 tmp2 = new Vector2();
		protected Vector2 prev = new Vector2();
		protected Vector2 heading = new Vector2();
		protected Vector2 pheading = new Vector2();

		public PositionInterpolator () {
		}

		public abstract Vector2 transform (Vector2 targetPosition, float zoom);
	}
}
