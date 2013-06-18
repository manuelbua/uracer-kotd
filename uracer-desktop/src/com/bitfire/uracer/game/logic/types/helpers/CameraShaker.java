
package com.bitfire.uracer.game.logic.types.helpers;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class CameraShaker {
	Vector2 result = new Vector2();
	InterpolatedFloat noiseX = new InterpolatedFloat();
	InterpolatedFloat noiseY = new InterpolatedFloat();
	BoxedFloat camshakeFactor = new BoxedFloat(0);
	float lastImpactForce = 0;

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
			GameTweener.start(Timeline.createSequence()
				.push(Tween.to(camshakeFactor, BoxedFloatAccessor.VALUE, 200).target(clampedImpactForce).ease(Linear.INOUT))
				.push(Tween.to(camshakeFactor, BoxedFloatAccessor.VALUE, 500).target(0).ease(Linear.INOUT))
				.setCallback(camShakeFinished));

			// Gdx.app.log("", "target force=" + NumberString.formatLong(clampedImpactForce));
		}
	}

	public Vector2 compute () {
		float alpha = AMath.fixup(0.2f * camshakeFactor.value);
		float pixels = 100;
		float radiusX = (MathUtils.random() - 0.5f) * 2 * pixels;
		float radiusY = (MathUtils.random() - 0.5f) * 2 * pixels;
		radiusX = MathUtils.random(-pixels, pixels);
		radiusY = MathUtils.random(-pixels, pixels);
		noiseX.set(radiusX, alpha);
		noiseY.set(radiusY, alpha);
		result.set(noiseX.get(), noiseY.get());
		return result;
	}
}
