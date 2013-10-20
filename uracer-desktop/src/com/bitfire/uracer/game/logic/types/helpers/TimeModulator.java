
package com.bitfire.uracer.game.logic.types.helpers;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Quad;

import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

public final class TimeModulator {

	public static final float MinTime = 0.25f;
	public static final float MaxTime = 1.0f;

	private BoxedFloat timeMultiplier;
	private Timeline timeSeq;

	public TimeModulator () {
		timeMultiplier = new BoxedFloat(1);
		timeSeq = Timeline.createSequence();
	}

	// returns the modulate time value
	public float getTime () {
		return AMath.clamp(timeMultiplier.value, MinTime, MaxTime);
	}

	public void reset () {
		timeMultiplier.value = MaxTime;
	}

	public void toDilatedTime () {
		modulateTo(Quad.OUT, MinTime, 1000);
	}

	public void toNormalTime () {
		modulateTo(Quad.OUT, MaxTime, 1000);
	}

	private void modulateTo (TweenEquation eq, float to, float durationMs) {
		SysTweener.stop(timeMultiplier);
		timeSeq = Timeline.createSequence();
		timeSeq.push(Tween.to(timeMultiplier, BoxedFloatAccessor.VALUE, durationMs).target(to).ease(eq));
		SysTweener.start(timeSeq);
	}
}
