
package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public class ScaleUtils {

	public static int RefScreenWidth, RefScreenHeight;
	public static int ScreenWidth, ScreenHeight;
	public static int PlayWidth, PlayHeight;
	public static int CropX, CropY;
	public static float Scale;

	// private static Vector2 ref2scr = new Vector2();

	public static void init (Vector2 refScreen) {
		ScreenWidth = Gdx.graphics.getWidth();
		ScreenHeight = Gdx.graphics.getHeight();
		RefScreenWidth = (int)refScreen.x;
		RefScreenHeight = (int)refScreen.y;

		// Maintain the aspect ratio by letterboxing.
		float refAspect = (float)RefScreenWidth / (float)RefScreenHeight;
		float physicalWidth = (float)ScreenWidth;
		float physicalHeight = (float)ScreenHeight;
		float aspect = physicalWidth / physicalHeight;

		CropX = 0;
		CropY = 0;
		if (aspect > refAspect) {

			// Letterbox left and right
			Scale = physicalHeight / (float)RefScreenHeight;
			CropX = (int)((physicalWidth - (float)RefScreenWidth * Scale) / 2f);

		} else if (aspect < refAspect) {

			// Letterbox above and below
			Scale = physicalWidth / (float)RefScreenWidth;
			CropX = 0;
			CropY = (int)((physicalHeight - (float)RefScreenHeight * Scale) / 2f);

		} else {
			Scale = 1;
		}

		PlayWidth = (int)(RefScreenWidth * Scale);
		PlayHeight = (int)(RefScreenHeight * Scale);

		Gdx.app.log("ScaleUtils", "Scale=" + Scale);
		Gdx.app.log("ScaleUtils", "Play=" + PlayWidth + "x" + PlayHeight);
		Gdx.app.log("ScaleUtils", "Crop=" + CropX + "x" + CropY);
	}
}
