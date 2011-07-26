package com.bitfire.uracer.utils;


/**
 * Algebra math utils.
 *
 * @author manuel
 *
 */

public class AMath
{
	public static final float CMP_EPSILON = 0.001f;

	public static final boolean equals(float a, float b)
	{
		if( a == b ) return true;
		return Math.abs( a-b ) < CMP_EPSILON;
	}

	public static final boolean isZero(float a)
	{
		return Math.abs(a) < CMP_EPSILON;
	}

	public static final float lerp( float prev, float curr, float alpha )
	{
		return curr * alpha + prev * (1f - alpha);
	}

	public static final float lowpass( float prev, float curr, float alpha )
	{
		return lerp( prev, curr, alpha );
	}

	public static final float hipass( float prev, float curr, float alpha )
	{
		return curr - lowpass( prev, curr, alpha );
	}

	public static final float modulo( float value, float div )
	{
		int result = (int)(value / div);
		return (value - (float)result * div);
	}

	public static final float wrap( float value, float lower, float upper )
	{
		float wrapped = modulo( value, (upper - lower) );
		return wrapped + lower;
	}

	public static final float wrap2PI( float value )
	{
		return wrap( value, 0.f, 6.28318530717958647692f );
	}

	public static final float clamp( float value, float min, float max )
	{
		return Math.min( max, Math.max( min, value ) );
	}

	public static final float fixup(float v)
	{
		if( Math.abs(v) < CMP_EPSILON) return 0;
		return v;
	}

	public static final float sign(float v)
	{
		if( v < 0 ) return -1f;
		return 1f;
	}
}
