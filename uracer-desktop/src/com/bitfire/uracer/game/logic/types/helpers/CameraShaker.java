
package com.bitfire.uracer.game.logic.types.helpers;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class CameraShaker {
	private static final int Pixels = (int)(Gdx.graphics.getWidth() / 6.6f);

	Vector2 result = new Vector2();
	InterpolatedFloat noiseX = new InterpolatedFloat();
	InterpolatedFloat noiseY = new InterpolatedFloat();
	public static final BoxedFloat camshakeFactor = new BoxedFloat(0);
	public static float lastImpactForce = 0;

	private TweenCallback camShakeFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				lastImpactForce = 0;
			}
		}
	};

	public void onCollision (CarEvent.Data data) {
		float clampedImpactForce = AMath.normalizeImpactForce(data.impulses.len());
		if (clampedImpactForce > 0 && clampedImpactForce > lastImpactForce) {
			// stop current shaking to do the next one *only* if heavier
			lastImpactForce = clampedImpactForce;
			GameTweener.stop(camshakeFactor);
			camshakeFactor.value = 0;
			GameTweener.start(Timeline
				.createSequence()
				.push(
					Tween.to(camshakeFactor, BoxedFloatAccessor.VALUE, 200).target(clampedImpactForce)
						.ease(aurelienribon.tweenengine.equations.Linear.INOUT))
				.push(
					Tween.to(camshakeFactor, BoxedFloatAccessor.VALUE, 500 + 1000 * clampedImpactForce).target(0).ease(Linear.INOUT))
				.setCallback(camShakeFinished));

			// Gdx.app.log("", "\ntarget force=" + NumberString.formatLong(clampedImpactForce));
		}
	}

	public Vector2 compute () {
		float alpha = AMath.fixup(camshakeFactor.value) * 0.05f;
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
