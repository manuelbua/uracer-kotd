package com.bitfire.uracer.effects.postprocessing.bloom;

public class BloomSettings
{
	public enum BlurType { GaussianBilinear, Gaussian, GaussianApproximation };
	public final String name;

	public final BlurType blurType;
	public final int blurPasses;	// simple blur
	public final float blurAmount;	// normal blur (1 pass)
	public final float bloomThreshold;

	public final float bloomIntensity;
	public final float bloomSaturation;
	public final float baseIntensity;
	public final float baseSaturation;

	public BloomSettings( String name, BlurType blurType, int blurPasses, float blurAmount, float bloomThreshold, float baseIntensity, float baseSaturation, float bloomIntensity, float bloomSaturation )
	{
		this.name = name;
		this.blurType = blurType;
		this.blurPasses = blurPasses;
		this.blurAmount = blurAmount;

		this.bloomThreshold = bloomThreshold;
		this.baseIntensity = baseIntensity;
		this.baseSaturation = baseSaturation;
		this.bloomIntensity = bloomIntensity;
		this.bloomSaturation = bloomSaturation;
	}

	// simple blur
	public BloomSettings( String name, int blurPasses, float bloomThreshold, float baseIntensity, float baseSaturation, float bloomIntensity, float bloomSaturation )
	{
		this( name, BlurType.GaussianApproximation, blurPasses, 0, bloomThreshold, baseIntensity, baseSaturation, bloomIntensity, bloomSaturation );
	}

	public BloomSettings(BloomSettings other)
	{
		this.name = other.name;
		this.blurType = other.blurType;
		this.blurPasses = other.blurPasses;
		this.blurAmount = other.blurAmount;

		this.bloomThreshold = other.bloomThreshold;
		this.baseIntensity = other.baseIntensity;
		this.baseSaturation = other.baseSaturation;
		this.bloomIntensity = other.bloomIntensity;
		this.bloomSaturation = other.bloomSaturation;
	}
}
