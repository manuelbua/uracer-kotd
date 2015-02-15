
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
import com.badlogic.gdx.utils.ObjectMap;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.utils.ShaderLoader;

public final class Art {
	public static TextureRegion[][] debugFont;

	// tileset friction maps
	public static Pixmap frictionMapDesert;

	// 3d
	public static Texture meshMissing;
	public static Texture meshTreeTrunk;
	public static ObjectMap<String, Texture> meshCar;
	public static Texture[] meshTreeLeavesSpring;
	public static Texture meshTrackWall;

	// cars
	public static TextureAtlas cars;
	public static TextureRegion skidMarksFront, skidMarksRear;
	public static Texture wrongWay;

	// fonts
	public static final int DebugFontWidth = 6;
	public static final int DebugFontHeight = 6;
	public static TextureAtlas fontAtlas;

	// post-processor
	// public static ShaderProgram depthMapGen, depthMapGenTransparent;
	public static Texture postXpro;
	public static Texture postLensFlare;

	// screens
	public static Texture scrBackground;
	public static Skin scrSkin;
	public static Texture scrPanel;
	private static TextureAtlas skinAtlas;

	// circle progress
	public static Texture texCircleProgress;
	public static Texture texCircleProgressMask;
	public static Texture texCircleProgressHalf;
	public static Texture texCircleProgressHalfMask;
	public static Texture texRadLinesProgress;

	// particle effects
	public static TextureAtlas particles;

	public static void init () {
		ShaderLoader.BasePath = "data/shaders/";
		loadFonts();
		loadCarGraphics();
		loadParticlesGraphics();
		loadMeshesGraphics(Config.Graphics.EnableMipMapping);
		loadFrictionMaps();
		loadPostProcessorMaps();
		loadScreensData();
		loadCircleProgress();
	}

	public static void dispose () {
		disposeFonts();
		disposeParticlesGraphics();
		disposeCarGraphics();
		disposeMeshesGraphics();
		disposeFrictionMaps();
		disposePostProcessorMaps();
		disposeScreensData();
		disposeCircleProgress();
	}

	//
	// circle progress
	//
	private static void loadCircleProgress () {
		texCircleProgress = Art.newTexture("data/base/progress/circle-progress-full.png", true);
		texCircleProgress.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);

		texCircleProgressHalf = Art.newTexture("data/base/progress/circle-progress-half.png", true);
		texCircleProgressHalf.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);

		texCircleProgressHalfMask = Art.newTexture("data/base/progress/circle-progress-half-mask.png", true);
		texCircleProgressHalfMask.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);

		texRadLinesProgress = Art.newTexture("data/base/progress/radlines-progress-full.png", true);
		texRadLinesProgress.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);

		texCircleProgressMask = Art.newTexture("data/base/progress/circle-progress-mask.png", true);
		texCircleProgressMask.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);
	}

	private static void disposeCircleProgress () {
		texCircleProgress.dispose();
		texCircleProgressMask.dispose();
		texRadLinesProgress.dispose();
		texCircleProgressHalf.dispose();
		texCircleProgressHalfMask.dispose();
	}

	//
	// screens
	//
	public static void loadScreensData () {
		scrBackground = newTexture("data/base/titlescreen.png", true);

		// the skin will automatically search and load the same filename+".atlas" extension

		// skinAtlas = new TextureAtlas("data/ui/skin/skin.atlas");
		// if (ScaleUtils.PlayWidth < 1280) {
		// scrSkin = new Skin(Gdx.files.internal(Storage.UI + "skin/skin-small.json"), skinAtlas);
		// } else if (ScaleUtils.PlayWidth >= 1280) { // && ScaleUtils.PlayWidth < 1440) {
		// scrSkin = new Skin(Gdx.files.internal(Storage.UI + "skin/skin-big.json"), skinAtlas);
		// }
		// old
		// else if (ScaleUtils.PlayWidth >= 1440) {
		// scrSkin = new Skin(Gdx.files.internal(Storage.UI + "skin-big.json"), skinAtlas);
		// }

		// // holo
		// String skinName = "Holo-dark-ldpi";
		// String skinPath = Storage.UI + "holo/" + skinName;
		// skinAtlas = new TextureAtlas(Gdx.files.internal(skinPath + ".atlas"));
		// scrSkin = new Skin(Gdx.files.internal(skinPath + ".json"), skinAtlas);

		// kenney
		String skinPath = Storage.UI + "kenney/";
		skinAtlas = new TextureAtlas(Gdx.files.internal(skinPath + "pack.atlas"));
		scrSkin = new Skin(Gdx.files.internal(skinPath + "kenney.json"), skinAtlas);

		// brushed texture
		scrPanel = newTexture("data/base/panel.png", false);
	}

	public static void disposeScreensData () {
		scrSkin.dispose();
		scrBackground.dispose();
		skinAtlas.dispose();
		scrPanel.dispose();
	}

	//
	// post processor maps
	//

	private static void loadPostProcessorMaps () {
		postXpro = newTexture("data/base/xpro-lut.png", false);
		postXpro.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

		postLensFlare = newTexture("data/base/lenscolor.png", false);
		postLensFlare.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

		// depthMapGen = ShaderLoader.fromFile( "depth", "depth" );
		// depthMapGenTransparent = ShaderLoader.fromFile( "depth-transparent",
		// "depth-transparent" );
	}

	private static void disposePostProcessorMaps () {
		postXpro.dispose();
		postLensFlare.dispose();
		// depthMapGenTransparent.dispose();
		// depthMapGen.dispose();
	}

	//
	// friction maps
	//

	private static void loadFrictionMaps () {
		// friction maps
		frictionMapDesert = new Pixmap(Gdx.files.internal("data/levels/tileset/desert-friction-easy.png"));
	}

	private static void disposeFrictionMaps () {
		frictionMapDesert.dispose();
	}

	//
	// meshes
	//

	private static void loadMeshesGraphics (boolean mipmap) {
		meshTrackWall = newTexture("data/track/wall_4.png", mipmap);
		meshTrackWall.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);

		meshMissing = newTexture("data/3d/textures/missing-mesh.png", mipmap);

		// car textures
		meshCar = new ObjectMap<String, Texture>();
		meshCar.put("car", newTexture("data/3d/textures/car.png", mipmap));
		meshCar.put("car_yellow", newTexture("data/3d/textures/car_yellow.png", mipmap));
		// meshCar.get("car").setFilter(TextureFilter.Linear, TextureFilter.Linear);
		// meshCar.get("car_yellow").setFilter(TextureFilter.Linear, TextureFilter.Linear);

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

		// car textures
		for (Texture t : meshCar.values()) {
			t.dispose();
		}
		meshCar.clear();

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

		wrongWay = newTexture("data/base/wrong-way.png", false);
		wrongWay.setFilter(TextureFilter.Linear, TextureFilter.Linear);
	}

	private static void disposeCarGraphics () {
		wrongWay.dispose();
		cars.dispose();
	}

	//
	// particles
	//

	private static void loadParticlesGraphics () {
		particles = new TextureAtlas("data/partfx/textures/pack.atlas");
	}

	private static void disposeParticlesGraphics () {
		particles.dispose();
	}

	//
	// fonts
	//

	private static void loadFonts () {
		// debug font, no need to scale it
		debugFont = split("data/base/debug-font.png", DebugFontWidth, DebugFontHeight, false);
		debugFont[0][0].getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);

		// game fonts
		fontAtlas = new TextureAtlas("data/font/pack.atlas");
	}

	private static void disposeFonts () {
		debugFont[0][0].getTexture().dispose();
		fontAtlas.dispose();
	}

	//
	// flags
	//

	public static Texture getFlag (String countryCode) {
		String filename = countryCode + ".png";
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

					boolean mipMap = false;
					Texture t = new Texture(px);

					if (mipMap) {
						t.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Nearest);
					} else {
						t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
					}

					px.dispose();
					return t;
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

	public static Texture newTexture (String name, boolean mipMap) {
		Texture t = new Texture(Gdx.files.internal(name), Format.RGBA8888, mipMap);

		if (mipMap) {
			t.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Nearest);
		} else {
			t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		}

		return t;
	}

	// hide constructor
	private Art () {
	}

}
