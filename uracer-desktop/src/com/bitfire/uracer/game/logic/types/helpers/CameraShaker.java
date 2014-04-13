
package com.bitfire.uracer.game.logic.types.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.ScaleUtils;

public final class CameraShaker {
	private static final int Pixels = 256;

	Vector2 result = new Vector2();
	InterpolatedFloat noiseX = new InterpolatedFloat();
	InterpolatedFloat noiseY = new InterpolatedFloat();

	public Vector2 compute (float factor) {
		float px = Pixels;
		px *= ScaleUtils.Scale;

		float radiusX = MathUtils.random(-px, px);
		float radiusY = MathUtils.random(-px, px);
		float noiseAlpha = 0.1f;// * factor;
		noiseX.set(radiusX, noiseAlpha);
		noiseY.set(radiusY, noiseAlpha);
		result.set(noiseX.get() * factor, noiseY.get() * factor);

		// int limit = 50;
		// result.x = MathUtils.clamp(result.x, -limit, limit);
		// result.y = MathUtils.clamp(result.y, -limit, limit);

		// Gdx.app.log("", result.toString() + " / " + factor);
		// Gdx.app.log("", "pixels=" + px);
		return result;
	}
}
