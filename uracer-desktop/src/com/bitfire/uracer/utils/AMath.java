
package com.bitfire.uracer.utils;

import com.bitfire.uracer.configuration.Config;

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
	public static final float ONE_ON_CMP_EPSILON = 1000f;

	private AMath () {
	}

	public static boolean equals (float a, float b) {
		return Math.abs(Math.abs(a) - Math.abs(b)) < CMP_EPSILON;
	}

	public static boolean isZero (float a) {
		return Math.abs(a) < CMP_EPSILON;
	}

	public static boolean isZero (double a) {
		return Math.abs(a) < CMP_EPSILON;
	}

	public static float lerp (float prev, float curr, float alpha) {
		return curr * alpha + prev * (1f - alpha);
	}

	public static float lowpass (float prev, float curr, float alpha) {
		return lerp(prev, curr, alpha);
	}

	public static float hipass (float prev, float curr, float alpha) {
		return curr - lowpass(prev, curr, alpha);
	}

	public static float modulo (float value, float div) {
		int result = (int)(value / div);
		return (value - (float)result * div);
	}

	public static float wrap (float value, float lower, float upper) {
		float wrapped = modulo(value, (upper - lower));
		return wrapped + lower;
	}

	public static float wrap2PI (float value) {
		return wrap(value, 0.f, TWO_PI);
	}

	public static float clamp (float value, float min, float max) {
		return Math.min(max, Math.max(min, value));
	}

	public static long clamp (long value, long min, long max) {
		return Math.min(max, Math.max(min, value));
	}

	public static int clamp (int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}

	public static float fixup (float v) {
		if (Math.abs(v) < CMP_EPSILON) {
			return 0;
		}

		return v;
	}

	public static float fixup (float v, float epsilon) {
		if (Math.abs(v) < epsilon) {
			return 0;
		}

		return v;
	}

	public static float fixupTo (float v, float target) {
		if (Math.abs(Math.abs(v) - Math.abs(target)) < CMP_EPSILON) {
			return target;
		}

		return v;
	}

	public static float fixupTo (float v, float target, float epsilon) {
		if (Math.abs(Math.abs(v) - Math.abs(target)) < epsilon) {
			return target;
		}

		return v;
	}

	public static float clampf (float v, float min, float max) {
		return AMath.clamp(AMath.fixupTo(AMath.fixupTo(v, min), max), min, max);
	}

	public static float clampf (float v, float min, float max, float epsilon) {
		return AMath.clamp(AMath.fixupTo(AMath.fixupTo(v, min, epsilon), max, epsilon), min, max);
	}

	public static float sign (float v) {
		if (v < 0) {
			return -1f;
		}

		return 1f;
	}

	public static float normalRelativeAngle (float angleRad) {
		float wrapped = (angleRad % TWO_PI);
		return wrapped >= 0 ? (wrapped < PI) ? wrapped : wrapped - TWO_PI : (wrapped >= -PI) ? wrapped : wrapped + TWO_PI;
	}

	public static float sigmoid (float strength) {
		return (float)(1f / (1f + Math.pow(Math.E, -strength)));
	}

	// lookup the "sigmoid" IPython notebook for graphing
	public static float sigmoidN (float x, float a) {
		return (float)(2f / (1f + Math.exp(-a * x))) - 1.0f;
	}

	// lookup the "sigmoid" IPython notebook for graphing
	public static float sigmoidN (float x) {
		return sigmoidN(x, 1);
	}

	private static final float SigmoidTAmplitude = 5; // good values in the range [3, 8]
	private static final float SigmoidTStepSize = 3;

	/** Tweakable normalized sigmoid function based on Dino Dini's implementation (see "sigmoid" IPython notebook)
	 * 
	 * Formulae: sign(x) * ( abs(x)*k / (1+k-abs(x)) )
	 * 
	 * @param x an input value in the range [-1,1]
	 * @param k an amplitude modulation epsilon in the range [0,1]
	 * @param up Whether the amplitude refers to the width or the height
	 * @return the modulated input value x */
	public static float sigmoidT (float x, float k, boolean up) {
		float sgn = x >= 0 ? 1 : -1;
		float absx = Math.abs(x);
		float absk = Math.abs(k);
		float n = absk * SigmoidTAmplitude;
		float _k = (1.0f / (float)Math.pow(SigmoidTStepSize, n)) * SigmoidTAmplitude;
		if (up) _k = -_k - 1;
		return sgn * ((absx * _k) / (1 + _k - absx));
	}

	public static float truncate (float value, int decimal) {
		float temp = value;

		switch (decimal) {
		case 1:
			temp = (int)(value * 10) / 10f;
			break;
		case 2:
			temp = (int)(value * 100) / 100f;
			break;
		case 3:
			temp = (int)(value * 1000) / 1000f;
			break;
		}

		return temp;
	}

	public static float round (float value, int decimal) {
		float p = (float)Math.pow(10, decimal);
		float tmp = Math.round(value * p);
		return (float)tmp / p;
	}

	public static float normalizeImpactForce (float force) {
		float v = AMath.clamp(force, 0, Config.Physics.MaxImpactForce);
		v *= Config.Physics.OneOnMaxImpactForce;
		return v;
	}

	//@off
	/**
	 *  Compute a timestep-dependent damping factor from the specified time-independent constant and arbitrary factor.
	 *
	 * This isn't the only way to compute it:
	 *
	 * 		let 0.975 be the time-DEPENDENT factor
	 * 			factor ^ (factor_good_at_timestep * dt)
	 * 				or
	 * 			pow( factor, factor_good_at_timestep * dt )
	 * 
	 * 		that is:
	 * 			pow( 0.975, 60 * dt )
	 * 		thus, for a 60hz timestep:
	 * 			pow( 0.975, 60 * (1/60) ) = 0.975			|	exp( -1.5 * (1/60) ) = 0.975309 (w/ 1.5 found by trial and error)
	 * 		and for a 30hz timestep:
	 * 			pow( 0.975, 60 * (1/30) ) = 0.950625		|	exp( -1.5 * (1/30) ) = 0.951229
	 *
	 * (see my post http://www.gamedev.net/topic/624172-framerate-independent-friction/page__st__20)
	 */
	//@on
	public static float damping (float factor) {
		return (float)Math.pow(factor, Config.Physics.PhysicsTimestepReferenceHz * Config.Physics.Dt);
		// return (float)Math.exp( -factor * Config.Physics.PhysicsDt );
	}
}
