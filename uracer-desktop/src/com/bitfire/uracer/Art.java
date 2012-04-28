package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

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
	public static BitmapFont fontCurseYR, fontCurseR, fontCurseG;
	public static BitmapFont fontCurseYRbig, fontCurseRbig, fontCurseGbig;

	private static TextureAtlas fontAtlas;

	public static void init( float invZoomFactor ) {
		loadFonts( invZoomFactor );
		loadCarGraphics();
		loadMeshesGraphics( Config.Graphics.EnableMipMapping );
		loadFrictionMaps();
	}

	public static void dispose() {
		disposeFonts();
		disposeCarGraphics();
		disposeMeshesGraphics();
		disposeFrictionMaps();
	}

	//
	// friction maps
	//

	private static void loadFrictionMaps() {
		// friction maps
		frictionNature = new Pixmap( Gdx.files.internal( "data/levels/tilesets/nature/224-friction.png" ) );
	}

	private static void disposeFrictionMaps() {
		frictionNature.dispose();
	}

	//
	// meshes
	//

	private static void loadMeshesGraphics( boolean mipmap ) {
		meshTrackWall = newTexture( "data/track/wall.png", false );
		meshMissing = newTexture( "data/3d/textures/missing-mesh.png", mipmap );
		meshPalm = newTexture( "data/3d/textures/palm.png", mipmap );
		meshTribune = newTexture( "data/3d/textures/tribune.png", mipmap );

		// trees
		meshTreeTrunk = newTexture( "data/3d/textures/trunk_6_col.png", mipmap );
		meshTreeLeavesSpring = new Texture[ 7 ];
		for( int i = 0; i < 7; i++ ) {
			meshTreeLeavesSpring[i] = newTexture( "data/3d/textures/leaves_" + (i + 1) + "_spring_1.png", mipmap );
		}
	}

	private static void disposeMeshesGraphics() {
		meshMissing.dispose();
		meshTrackWall.dispose();
		meshPalm.dispose();
		meshTribune.dispose();

		// trees
		for( int i = 0; i < 7; i++ ) {
			meshTreeLeavesSpring[i].dispose();
		}

		meshTreeTrunk.dispose();
	}

	//
	// cars
	//

	private static void loadCarGraphics() {
		cars = new TextureAtlas( "data/cars/pack" );

		skidMarksFront = cars.findRegion( "skid-marks-front" );
		skidMarksRear = cars.findRegion( "skid-marks-rear" );
		carAmbientOcclusion = cars.findRegion( "car-ao" );
	}

	private static void disposeCarGraphics() {
		cars.dispose();
	}

	//
	// fonts
	//

	private static void loadFonts( float scale ) {
		// debug font, no need to scale it
		debugFont = split( "data/base/debug-font.png", DebugFontWidth, DebugFontHeight, false );

		// game fonts
		fontAtlas = new TextureAtlas( "data/font/pack" );
		for( TextureRegion r : fontAtlas.getRegions() ) {
			r.getTexture().setFilter( TextureFilter.Linear, TextureFilter.Linear );
		}

		// default size
		fontCurseYR = new BitmapFont( Gdx.files.internal( "data/font/curse-y-r.fnt" ), Art.fontAtlas.findRegion( "curse-y-r" ), true );
		fontCurseG = new BitmapFont( Gdx.files.internal( "data/font/curse-g.fnt" ), Art.fontAtlas.findRegion( "curse-g" ), true );
		fontCurseR = new BitmapFont( Gdx.files.internal( "data/font/curse-r.fnt" ), Art.fontAtlas.findRegion( "curse-r" ), true );

		// big size
		fontCurseYRbig = new BitmapFont( Gdx.files.internal( "data/font/curse-y-r-big.fnt" ), Art.fontAtlas.findRegion( "curse-y-r-big" ), true );
		fontCurseGbig = new BitmapFont( Gdx.files.internal( "data/font/curse-g-big.fnt" ), Art.fontAtlas.findRegion( "curse-g-big" ), true );
		fontCurseRbig = new BitmapFont( Gdx.files.internal( "data/font/curse-r-big.fnt" ), Art.fontAtlas.findRegion( "curse-r-big" ), true );

		// adjust scaling
		fontCurseYR.setScale( scale );
		fontCurseG.setScale( scale );
		fontCurseR.setScale( scale );
		fontCurseYRbig.setScale( scale );
		fontCurseGbig.setScale( scale );
		fontCurseRbig.setScale( scale );
	}

	private static void disposeFonts() {
		debugFont[0][0].getTexture().dispose();
		fontAtlas.dispose();
	}

	//
	// helpers
	//

	private static TextureRegion[][] split( String name, int width, int height, boolean mipMap ) {
		return split( name, width, height, false, true, mipMap );
	}

	private static TextureRegion[][] split( String name, int width, int height, boolean flipX, boolean flipY, boolean mipMap ) {
		Texture texture = newTexture( name, mipMap );
		int xSlices = texture.getWidth() / width;
		int ySlices = texture.getHeight() / height;
		TextureRegion[][] res = new TextureRegion[ xSlices ][ ySlices ];
		for( int x = 0; x < xSlices; x++ ) {
			for( int y = 0; y < ySlices; y++ ) {
				res[x][y] = new TextureRegion( texture, x * width, y * height, width, height );
				res[x][y].flip( flipX, flipY );
			}
		}
		return res;
	}

	private static Texture newTexture( String name, boolean mipMap ) {
		Texture t = new Texture( Gdx.files.internal( name ), Format.RGBA4444, mipMap );

		if( mipMap ) {
			t.setFilter( TextureFilter.MipMapLinearNearest, TextureFilter.Nearest );
		} else {
			t.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
		}

		return t;
	}

	// hides constructor
	private Art() {
	}

}
