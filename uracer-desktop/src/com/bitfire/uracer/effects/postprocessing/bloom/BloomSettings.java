package com.bitfire.uracer.effects.postprocessing.bloom;

public class BloomSettings
{
	public final String name;

	public final int blurPasses;
	public final float bloomThreshold;

	public final float bloomIntensity;
	public final float bloomSaturation;
	public final float baseIntensity;
	public final float baseSaturation;

	public BloomSettings( String name, int blurPasses, float bloomThreshold, float baseIntensity, float baseSaturation, float bloomIntensity, float bloomSaturation )
	{
		this.name = name;
		this.blurPasses = blurPasses;
		this.bloomThreshold = bloomThreshold;
		this.baseIntensity = baseIntensity;
		this.baseSaturation = baseSaturation;
		this.bloomIntensity = bloomIntensity;
		this.bloomSaturation = bloomSaturation;
	}
}
