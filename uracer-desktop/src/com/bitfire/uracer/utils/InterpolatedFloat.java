
package com.bitfire.uracer.utils;

/** Encpasulates a floating point value interpolated towards the specified target value by the specified alpha amount */
public class InterpolatedFloat {
	private float previous, current;
	private boolean fixup;

	public InterpolatedFloat () {
		this(0, true);
	}

	public InterpolatedFloat (float value, boolean fixup) {
		this.previous = value;
		this.current = value;
		this.fixup = fixup;
	}

	public float set (float value, float alpha) {
		current = AMath.lerp(previous, value, alpha);
		if (fixup) {
			current = AMath.fixup(current);
		}
		previous = current;
		return current;
	}

	public float get () {
		return current;
	}
}
