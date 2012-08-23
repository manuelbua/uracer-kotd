
package com.bitfire.uracer.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.utils.Hash;

/** Lazy factory for BitmapFont objects
 * 
 * Used font resources number count is usually fairy low, so let's assume this and implement a "lazy loading" strategy: cache-hits
 * should be early maximized (100%) just at the first couple of framesof the real game */
public final class BitmapFontFactory {
	public enum FontFace {
		//@off
		AdobeSourceSans("adobe-source-sans"),
		CurseGreen("curse-g"),
		CurseGreenBig("curse-g-big"),
		CurseRed("curse-r"),
		CurseRedBig("curse-r-big"),
		CurseRedYellow("curse-y-r"),
		CurseRedYellowBig("curse-y-r-big"),
		Molengo("molengo"),
		Roboto("roboto"),
		CurseRedYellowNew("curse-new"),
		Lcd("lcd")
		;
		//@on

		// the name reflects the filename
		public String name;

		FontFace (String name) {
			this.name = name;
		}
	}

	private static ScalingStrategy scalingStrategy;

	// storage
	private static LongMap<BitmapFont> fontCache = new LongMap<BitmapFont>();

	public static void init (ScalingStrategy strategy) {
		scalingStrategy = strategy;
	}

	/** Returns an instance of a BitmapFont, performing a font reset to the initial state (when reused), if specified. */
	public static BitmapFont get (FontFace face, boolean reset) {
		String name = face.name;
		long hash = Hash.APHash(name);
		BitmapFont f = fontCache.get(hash);

		if (f != null) {
// Gdx.app.log("BitmapFontFactory", "Cache hit for \"" + name + "\"");
			if (reset) {
				setupFont(f);
			}
			return f;
		}

		f = new BitmapFont(Gdx.files.internal("data/font/" + name + ".fnt"), Art.fontAtlas.findRegion(name), true);

		setupFont(f);

		fontCache.put(hash, f);

		return f;
	}

	/** Commodity helper */
	public static BitmapFont get (FontFace face) {
		return get(face, true);
	}

	private static void setupFont (BitmapFont font) {
		font.setScale(scalingStrategy.invTileMapZoomFactor);
		font.setUseIntegerPositions(false);
// font.setFixedWidthGlyphs("1234567890.");
	}

	// FIXME i don't like to not be able to inherit from Disposable.. ;-/
	public static void dispose () {
		for (BitmapFont f : fontCache.values()) {
			f.dispose();
		}

		fontCache.clear();
	}

	private BitmapFontFactory () {
	}
}
