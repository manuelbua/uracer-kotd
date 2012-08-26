
package com.bitfire.uracer.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.utils.ShaderLoader;

public final class Art {
	public static TextureRegion[][] debugFont;

	// tileset friction maps
	public static Pixmap frictionNature;

	// 3d
	public static Texture meshMissing;
	public static Texture meshPalm;
	public static Texture meshTribune;
	public static Texture meshTreeTrunk;
	public static Texture[] meshTreeLeavesSpring;
	public static Texture meshTrackWall;

	// cars
	public static TextureAtlas cars;
	public static TextureRegion carAmbientOcclusion;
	public static TextureRegion skidMarksFront, skidMarksRear;

	// fonts
	public static final int DebugFontWidth = 6;
	public static final int DebugFontHeight = 6;
	public static TextureAtlas fontAtlas;

	// post-processor
	// public static ShaderProgram depthMapGen, depthMapGenTransparent;
	public static Texture postXpro;

	// screns
	public static Texture scrBackground;
	public static Skin scrSkin;

	public static void init () {
		ShaderLoader.BasePath = "data/shaders/";
		loadFonts();
		loadCarGraphics();
		loadMeshesGraphics(Config.Graphics.EnableMipMapping);
		loadFrictionMaps();
		loadPostProcessorMaps();
		loadScreensData();
	}

	public static void dispose () {
		disposeFonts();
		disposeCarGraphics();
		disposeMeshesGraphics();
		disposeFrictionMaps();
		disposePostProcessorMaps();
		disposeScreensData();
	}

	//
	// screens
	//
	private static void loadScreensData () {
		scrBackground = newTexture("data/base/titlescreen.png", false);
		// the skin will automatically search and load the same filename+".atlas" extension
		scrSkin = new Skin(Gdx.files.internal(Storage.UI + "skin.json"));
	}

	private static void disposeScreensData () {
		scrSkin.dispose();
		scrBackground.dispose();
	}

	//
	// post processor maps
	//

	private static void loadPostProcessorMaps () {
		postXpro = newTexture("data/base/xpro-lut.png", false);
		postXpro.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

		// depthMapGen = ShaderLoader.fromFile( "depth", "depth" );
		// depthMapGenTransparent = ShaderLoader.fromFile( "depth-transparent",
		// "depth-transparent" );
	}

	private static void disposePostProcessorMaps () {
		postXpro.dispose();
		// depthMapGenTransparent.dispose();
		// depthMapGen.dispose();
	}

	//
	// friction maps
	//

	private static void loadFrictionMaps () {
		// friction maps
		frictionNature = new Pixmap(Gdx.files.internal("data/levels/tilesets/nature/224-friction.png"));
	}

	private static void disposeFrictionMaps () {
		frictionNature.dispose();
	}

	//
	// meshes
	//

	private static void loadMeshesGraphics (boolean mipmap) {
		meshTrackWall = newTexture("data/track/wall.png", false);
		meshTrackWall.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

		meshMissing = newTexture("data/3d/textures/missing-mesh.png", mipmap);
		meshPalm = newTexture("data/3d/textures/palm.png", mipmap);
		meshTribune = newTexture("data/3d/textures/tribune.png", mipmap);

		// trees
		meshTreeTrunk = newTexture("data/3d/textures/trunk_6_col.png", mipmap);
		meshTreeLeavesSpring = new Texture[7];
		for (int i = 0; i < 7; i++) {
			meshTreeLeavesSpring[i] = newTexture("data/3d/textures/leaves_" + (i + 1) + "_spring_1.png", mipmap);
		}
	}

	private static void disposeMeshesGraphics () {
		meshMissing.dispose();
		meshTrackWall.dispose();
		meshPalm.dispose();
		meshTribune.dispose();

		// trees
		for (int i = 0; i < 7; i++) {
			meshTreeLeavesSpring[i].dispose();
		}

		meshTreeTrunk.dispose();
	}

	//
	// cars
	//

	private static void loadCarGraphics () {
		cars = new TextureAtlas("data/cars/pack.atlas");

		skidMarksFront = cars.findRegion("skid-marks-front");
		skidMarksRear = cars.findRegion("skid-marks-rear");
		carAmbientOcclusion = cars.findRegion("car-ao");
	}

	private static void disposeCarGraphics () {
		cars.dispose();
	}

	//
	// fonts
	//

	private static void loadFonts () {
		// debug font, no need to scale it
		debugFont = split("data/base/debug-font.png", DebugFontWidth, DebugFontHeight, false);

		// game fonts
		fontAtlas = new TextureAtlas("data/font/pack.atlas");
		for (TextureRegion r : fontAtlas.getRegions()) {
			r.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		}
	}

	private static void disposeFonts () {
		debugFont[0][0].getTexture().dispose();
		fontAtlas.dispose();
	}

	//
	// flags
	//

	public static Texture getFlag (String filename) {
		FileHandle zip = Gdx.files.internal("data/flags.zip");
		ZipInputStream zin = new ZipInputStream(zip.read());
		ZipEntry ze = null;
		try {
			while ((ze = zin.getNextEntry()) != null) {
				if (ze.getName().equals(filename)) {
					ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
					int bytesRead;
					byte[] tempBuffer = new byte[8192 * 2];
					while ((bytesRead = zin.read(tempBuffer)) != -1) {
						streamBuilder.write(tempBuffer, 0, bytesRead);
					}

					Pixmap px = new Pixmap(streamBuilder.toByteArray(), 0, streamBuilder.size());

					streamBuilder.close();
					zin.close();

					return new Texture(px);
				}
			}
		} catch (IOException e) {
		}

		return null;
	}

	//
	// helpers
	//

	private static TextureRegion[][] split (String name, int width, int height, boolean mipMap) {
		return split(name, width, height, false, true, mipMap);
	}

	private static TextureRegion[][] split (String name, int width, int height, boolean flipX, boolean flipY, boolean mipMap) {
		Texture texture = newTexture(name, mipMap);
		int xSlices = texture.getWidth() / width;
		int ySlices = texture.getHeight() / height;
		TextureRegion[][] res = new TextureRegion[xSlices][ySlices];
		for (int x = 0; x < xSlices; x++) {
			for (int y = 0; y < ySlices; y++) {
				res[x][y] = new TextureRegion(texture, x * width, y * height, width, height);
				res[x][y].flip(flipX, flipY);
			}
		}
		return res;
	}

	private static Texture newTexture (String name, boolean mipMap) {
		Texture t = new Texture(Gdx.files.internal(name), Format.RGBA8888, mipMap);

		if (mipMap) {
			t.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Nearest);
		} else {
			t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		}

		return t;
	}

	// hides constructor
	private Art () {
	}

}
