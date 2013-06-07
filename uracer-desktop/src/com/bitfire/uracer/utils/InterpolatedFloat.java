
package com.bitfire.uracer.utils;

/** Encpasulates a floating point value interpolated towards the specified target value by the specified alpha amount */
public class InterpolatedFloat {
	private float reset;
	private float previous, current;
	private boolean fixup;

	public InterpolatedFloat () {
		this(0, true);
	}

	public InterpolatedFloat (float value, boolean fixup) {
		this.reset = value;
		this.previous = value;
		this.current = value;
		this.fixup = fixup;
	}

	public void setFixup (boolean fixup) {
		this.fixup = fixup;
	}

	public void reset (boolean resetState) {
		reset(reset, resetState);
	}

	public void reset (float value, boolean resetState) {
		if (resetState) {
			this.previous = value;
		}

		this.current = value;
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
