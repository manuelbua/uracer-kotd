package com.bitfire.uracer.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class ScalingStrategy {
	public final Vector2 referenceScreen;
	public float desiredHorizontalFov;
	public float verticalFov;
	public float tileMapZoomFactor;
	public float tileMapZoomFactorAtRef;
	public float invTileMapZoomFactor;
	public float forTileSize;
	public float targetScreenRatio;
	public float meshScaleFactor;
	public float tileSizeAtRef;

	public float hFovScalingFactor;
	public float to256;

	public ScalingStrategy( Vector2 referenceScreen, float desiredHorizontalFov, int forTileSize, float tileMapZoomAtRef ) {
		this.referenceScreen = new Vector2();
		this.referenceScreen.set( referenceScreen );

		this.forTileSize = (float)forTileSize;
		this.tileMapZoomFactorAtRef = tileMapZoomAtRef;

		this.tileSizeAtRef = this.forTileSize * this.tileMapZoomFactorAtRef;
		this.desiredHorizontalFov = desiredHorizontalFov;

		// reference scaling (256 to current)
		to256 = this.tileSizeAtRef / 256f;

		// compute scaling factors
		update();
	}

	private void update() {
		float thisW = (float)Gdx.graphics.getWidth();
		float thisH = (float)Gdx.graphics.getHeight();

		// compute tilemap zoom factor (ref:1=this:x)
		if( thisW > thisH || thisW == thisH ) {
			tileMapZoomFactor = 1f / ((thisW * tileMapZoomFactorAtRef) / referenceScreen.x);
		}
		else {
			tileMapZoomFactor = 1f / ((thisH * tileMapZoomFactorAtRef) / referenceScreen.y);
		}

		verticalFov = verticalFov( thisW, thisH, desiredHorizontalFov );
		targetScreenRatio = referenceScreen.x / thisW;
		invTileMapZoomFactor = 1f / tileMapZoomFactor;

		meshScaleFactor = (1f / (tileMapZoomFactor * tileMapZoomFactorAtRef)) * targetScreenRatio;

		// adjust to the actual hfov
		hFovScalingFactor = hFovToScalingFactor();
		meshScaleFactor *= hFovScalingFactor;

		System.out.println( "vfov=" + verticalFov + ", hfov=" + desiredHorizontalFov + ", msf=" + meshScaleFactor + ", tmzf="
				+ tileMapZoomFactor );
	}

	// http://rjdown.co.uk/projects/bfbc2/fovcalculator.php
	private static float verticalFov( float width, float height, float desiredHfovDeg ) {
		float radHfov = desiredHfovDeg * MathUtils.degreesToRadians;
		float vfov = (float)(2f * Math.atan( Math.tan( radHfov / 2f ) * (height / width) ));
		vfov *= MathUtils.radiansToDegrees;
		return vfov;
	}

	private float hFovToScalingFactor() {
		// I DON'T LIKE THIS
		//
		// 4th degree polynomial approximation (quartic)
		//
		// this has been computed in Wolfram Alpha, with the following
		// fitting description:
		// fit {{10,0.1188},{20,0.239},{30,0.363},{40,0.494},{50,0.63},{60,
		// 0.78}, {73, 1}, {80, 1.13},{90,1.355},{110,1.93},{120,2.35}}
		//
		// so this will return interpolated values for any given hfov in the range
		// [10,120] with quartic approximation
		return 1.15197f * 0.00000001f
				* (desiredHorizontalFov * desiredHorizontalFov * desiredHorizontalFov * desiredHorizontalFov) - 1.6847f
				* 0.000001f * (desiredHorizontalFov * desiredHorizontalFov * desiredHorizontalFov) + 0.000124545f
				* (desiredHorizontalFov * desiredHorizontalFov) + 0.00877285f * desiredHorizontalFov + 0.022265f;
	}

	public void setHorizontalFov( float desiredHfov ) {
		desiredHorizontalFov = desiredHfov;
		update();
	}
}
