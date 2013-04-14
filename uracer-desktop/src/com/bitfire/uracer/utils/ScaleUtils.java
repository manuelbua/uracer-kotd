
package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;

public class ScaleUtils {

	public static int RefScreenWidth, RefScreenHeight;
	public static int ScreenWidth, ScreenHeight;
	public static int PlayWidth, PlayHeight;
	public static float /* ScaleX, ScaleY, */Scale;

	// private static Vector2 ref2scr = new Vector2();

	public static void init (Vector2 refScreen) {
		ScreenWidth = Gdx.graphics.getWidth();
		ScreenHeight = Gdx.graphics.getHeight();
		RefScreenWidth = (int)refScreen.x;
		RefScreenHeight = (int)refScreen.y;

		// ref2scr = new Vector2((float)ScreenWidth / (float)RefScreenWidth, (float)ScreenHeight / (float)RefScreenHeight);
		// ScaleX = ref2scr.x;
		// ScaleY = ref2scr.y;

		// Maintain the aspect ratio by letterboxing.
		float refAspect = RefScreenWidth / RefScreenHeight;
		float physicalWidth = ScreenWidth;
		float physicalHeight = ScreenHeight;
		float aspect = physicalWidth / physicalHeight;

		if (aspect > refAspect) {
			// Letterbox left and right
			Scale = physicalHeight / RefScreenHeight;
		} else {
			// Letterbox above and below
			Scale = physicalHeight / physicalWidth;
		}

		PlayWidth = (int)(RefScreenWidth * Scale);
		PlayHeight = (int)(RefScreenHeight * Scale);

		Gdx.app.log("ScaleUtils", "Scale=" + Scale);
		Gdx.app.log("ScaleUtils", "Play=" + PlayWidth + "x" + PlayHeight);
	}
}
