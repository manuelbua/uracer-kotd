
package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ColorUtils {
	private static final Color tmpcolor = new Color();

	// @param amount in the [0,1] range (note that it's inclusive)
	public static Color paletteRYG (float amount, float alpha) {
		float greenRatio = MathUtils.clamp(amount, 0.23f, 1);
		float rbRange = (1 - MathUtils.clamp(greenRatio, 0.761f, 1)) / (1 - 0.761f);

		tmpcolor.r = 0.678f + (0.969f - 0.678f) * rbRange;
		tmpcolor.g = greenRatio;
		tmpcolor.b = 0.118f - (0.118f - 0.114f) * rbRange;
		tmpcolor.a = alpha;
		return tmpcolor;
	}
}
