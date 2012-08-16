
package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public final class SpriteBatchUtils {
	private static String[] chars = {"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", ".,!?:;\"'+-=/\\< "};
	private static TextureRegion[][] debugFont;
	private static int debugFontW;

	private SpriteBatchUtils () {
	}

	public static void init (TextureRegion[][] debugFont, int debugFontWidth) {
		SpriteBatchUtils.debugFont = debugFont;
		SpriteBatchUtils.debugFontW = debugFontWidth;
	}

	public static void draw (SpriteBatch batch, TextureRegion region, float x, float y) {
		int width = region.getRegionWidth();
		if (width < 0) {
			width = -width;
		}

		batch.draw(region, x, y, width, region.getRegionHeight());
	}

	public static void draw (SpriteBatch batch, TextureRegion region, float x, float y, float width, float height) {
		batch.draw(region, x, y, width, height);
	}

	public static void drawString (SpriteBatch batch, String string, float x, float y) {
		String upstring = string.toUpperCase();
		for (int i = 0; i < string.length(); i++) {
			char ch = upstring.charAt(i);
			for (int ys = 0; ys < chars.length; ys++) {
				int xs = chars[ys].indexOf(ch);
				if (xs >= 0) {
					draw(batch, debugFont[xs][ys], x + i * debugFontW, y);
				}
			}
		}
	}

	public static void drawString (SpriteBatch batch, String string, float x, float y, float w, float h) {
		String upstring = string.toUpperCase();
		for (int i = 0; i < string.length(); i++) {
			char ch = upstring.charAt(i);
			for (int ys = 0; ys < chars.length; ys++) {
				int xs = chars[ys].indexOf(ch);
				if (xs >= 0) {
					draw(batch, debugFont[xs][ys], x + i * w, y, w, h);
				}
			}
		}
	}

}
