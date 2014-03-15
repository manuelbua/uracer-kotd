
package com.bitfire.uracer.game.logic.types.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class CameraShaker {
	private static final int Pixels = 256;

	Vector2 result = new Vector2();
	InterpolatedFloat noiseX = new InterpolatedFloat();
	InterpolatedFloat noiseY = new InterpolatedFloat();

	public Vector2 compute (float factor) {
		// collisionFactor = 0.25f;
		float alpha = /* AMath.fixup */(factor) * 0.8f;
		float px = Pixels;

		// if (camshakeFactor.value > 0) {
		// Gdx.app.log("", "camshakeFactor=" + NumberString.formatLong(camshakeFactor.value));
		// }

		float radiusX = MathUtils.random(-px, px);
		float radiusY = MathUtils.random(-px, px);
		noiseX.set(radiusX, alpha);
		noiseY.set(radiusY, alpha);
		result.set(noiseX.get() * factor, noiseY.get() * factor);

		result.x = MathUtils.clamp(result.x, -50, 50);
		result.y = MathUtils.clamp(result.y, -50, 50);
		// result.clamp(-50, 50);

		// result.set(noiseX.get(), noiseY.get());
		Gdx.app.log("", result.toString() + " / " + factor);
		return result;
	}
}
