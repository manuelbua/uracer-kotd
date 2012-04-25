package com.bitfire.uracer.utils;

/** Algebra math utils.
 *
 * @author manuel */

public final class AMath {
	public static final float TWO_PI = 6.28318530717958647692f;
	public static final float PI = 3.14159265358979323846f;
	public static final float PI_2 = 1.57079632679489661923f;
	public static final float PI_4 = 0.785398163397448309616f;
	public static final float PI_8 = 0.392699081698724154807f;

	public static final float CMP_EPSILON = 0.001f;

	private AMath() {
	}

	public static boolean equals( float a, float b ) {

		return Math.abs( Math.abs( a ) - Math.abs( b ) ) < CMP_EPSILON;
	}

	public static boolean isZero( float a ) {
		return Math.abs( a ) < CMP_EPSILON;
	}

	public static float lerp( float prev, float curr, float alpha ) {
		return curr * alpha + prev * (1f - alpha);
	}

	public static float lowpass( float prev, float curr, float alpha ) {
		return lerp( prev, curr, alpha );
	}

	public static float hipass( float prev, float curr, float alpha ) {
		return curr - lowpass( prev, curr, alpha );
	}

	public static float modulo( float value, float div ) {
		int result = (int)(value / div);
		return (value - (float)result * div);
	}

	public static float wrap( float value, float lower, float upper ) {
		float wrapped = modulo( value, (upper - lower) );
		return wrapped + lower;
	}

	public static float wrap2PI( float value ) {
		return wrap( value, 0.f, TWO_PI );
	}

	public static float clamp( float value, float min, float max ) {
		return Math.min( max, Math.max( min, value ) );
	}

	public static int clamp( int value, int min, int max ) {
		return Math.min( max, Math.max( min, value ) );
	}

	public static float fixup( float v ) {
		if( Math.abs( v ) < CMP_EPSILON ) {
			return 0;
		}

		return v;
	}

	public static float sign( float v ) {
		if( v < 0 ) {
			return -1f;
		}

		return 1f;
	}

	public static float normalRelativeAngle( float angleRad ) {
		float wrapped = (angleRad % TWO_PI);
		return wrapped >= 0 ? (wrapped < PI) ? wrapped : wrapped - TWO_PI : (wrapped >= -PI) ? wrapped : wrapped + TWO_PI;
	}

	public static float sigmoid( float strength ) {
		return (float)(1f / (1f + Math.pow( Math.E, -strength )));
	}
}
