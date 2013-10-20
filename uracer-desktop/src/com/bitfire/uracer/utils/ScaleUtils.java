
package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.bitfire.uracer.configuration.Config;

public class ScaleUtils {

	public static int ScreenWidth, ScreenHeight;
	public static int PlayWidth, PlayHeight;
	public static int CropX, CropY;
	public static Rectangle PlayViewport;
	public static float Scale;
	public static float RefAspect;

	public static void init (int displayWidth, int displayHeight) {
		ScreenWidth = displayWidth;
		ScreenHeight = displayHeight;
		float refW = Config.Graphics.ReferenceScreenWidth;
		float refH = Config.Graphics.ReferenceScreenHeight;

		// Maintain the aspect ratio by letterboxing.
		RefAspect = refW / refH;
		float physicalWidth = (float)ScreenWidth;
		float physicalHeight = (float)ScreenHeight;
		float aspect = physicalWidth / physicalHeight;

		CropX = 0;
		CropY = 0;
		if (aspect > RefAspect) {

			// Letterbox left and right
			Scale = physicalHeight / refH;
			CropX = (int)((physicalWidth - refW * Scale) / 2f);

		} else if (aspect < RefAspect) {

			// Letterbox above and below
			Scale = physicalWidth / refW;
			CropY = (int)((physicalHeight - refH * Scale) / 2f);

		} else {
			Scale = physicalWidth / refW;
		}

		PlayWidth = (int)(refW * Scale);
		PlayHeight = (int)(refH * Scale);

		PlayViewport = new Rectangle(CropX, CropY, PlayWidth, PlayHeight);

		Gdx.app.log("ScaleUtils", "Scale=" + Scale);
		Gdx.app.log("ScaleUtils", "Play=" + PlayWidth + "x" + PlayHeight);
		Gdx.app.log("ScaleUtils", "Crop=" + CropX + "x" + CropY);
	}
}
