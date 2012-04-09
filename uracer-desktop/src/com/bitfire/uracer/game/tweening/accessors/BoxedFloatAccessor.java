package com.bitfire.uracer.game.tweening.accessors;

import aurelienribon.tweenengine.TweenAccessor;

import com.bitfire.uracer.utils.BoxedFloat;

public class BoxedFloatAccessor implements TweenAccessor<BoxedFloat> {
	public static final int VALUE = 1;

	@Override
	public int getValues( BoxedFloat target, int tweenType, float[] returnValues ) {
		switch( tweenType ) {
		default:
		case VALUE:
			returnValues[0] = target.value;
			return 1;
		}
	}

	@Override
	public void setValues( BoxedFloat target, int tweenType, float[] newValues ) {
		switch( tweenType ) {
		default:
		case VALUE:
			target.value = newValues[0];
		}
	}
}
