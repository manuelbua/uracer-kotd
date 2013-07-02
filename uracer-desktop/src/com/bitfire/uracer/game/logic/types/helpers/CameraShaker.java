
package com.bitfire.uracer.game.logic.types.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class CameraShaker {
	private static final int Pixels = (int)(Gdx.graphics.getWidth() / 3f);

	Vector2 result = new Vector2();
	InterpolatedFloat noiseX = new InterpolatedFloat();
	InterpolatedFloat noiseY = new InterpolatedFloat();

	public Vector2 compute (float collisionFactor) {
		float alpha = AMath.fixup(collisionFactor) * 0.03f;
		float px = Pixels;

		// if (camshakeFactor.value > 0) {
		// Gdx.app.log("", "camshakeFactor=" + NumberString.formatLong(camshakeFactor.value));
		// }

		float radiusX = MathUtils.random(-px, px);
		float radiusY = MathUtils.random(-px, px);
		noiseX.set(radiusX, alpha);
		noiseY.set(radiusY, alpha);
		result.set(noiseX.get(), noiseY.get());
		return result;
	}
}
