package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Art
{
	public static TextureRegion[][] base6;
	public static TextureRegion quad;

	public static TextureAtlas fonts;

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
	public static TextureAtlas carTextures;
	public static TextureAtlas cars;
	public static TextureRegion carAmbientOcclusion;
	public static TextureRegion skidMarksFront, skidMarksRear;

	// fonts
	public static BitmapFont fontCurseYR, fontCurseR, fontCurseG;
	public static BitmapFont fontCurseYRbig, fontCurseRbig, fontCurseGbig;

	private static boolean mipMap;

	public static void load()
	{
		mipMap = Config.Graphics.EnableMipMapping;

		base6 = split( "data/base/base6.png", 6, 6, mipMap );
		quad = new TextureRegion( base6[0][10], 0, 0, 18, 18 );

		meshTrackWall = newTexture( "data/track/wall.png", false );
		meshMissing = newTexture( "data/3d/textures/missing-mesh.png", mipMap );
		meshPalm = newTexture( "data/3d/textures/palm.png", mipMap );
		meshTribune = newTexture( "data/3d/textures/tribune.png", mipMap );

		// trees
		meshTreeTrunk = newTexture( "data/3d/textures/trunk_6_col.png", mipMap );
		meshTreeLeavesSpring = new Texture[7];
		for(int i = 0; i < 7; i++) meshTreeLeavesSpring[i] = newTexture( "data/3d/textures/leaves_" + (i+1) + "_spring_1.png", mipMap );

		// cars
		carTextures = new TextureAtlas("data/cars/pack");

		cars = carTextures;
		skidMarksFront = carTextures.findRegion( "skid-marks-front" );
		skidMarksRear = carTextures.findRegion( "skid-marks-rear" );
		carAmbientOcclusion = carTextures.findRegion( "car-ao" );

		// fonts
		fonts = new TextureAtlas( "data/font/pack" );
		for( TextureRegion r : fonts.getRegions() )
		{
			r.getTexture().setFilter( TextureFilter.Linear, TextureFilter.Linear );
		}

		// default size
		fontCurseYR = new BitmapFont( Gdx.files.internal( "data/font/curse-y-r.fnt" ), Art.fonts.findRegion( "curse-y-r" ), true );
		fontCurseG = new BitmapFont( Gdx.files.internal( "data/font/curse-g.fnt" ), Art.fonts.findRegion( "curse-g" ), true );
		fontCurseR = new BitmapFont( Gdx.files.internal( "data/font/curse-r.fnt" ), Art.fonts.findRegion( "curse-r" ), true );

		// big size
		fontCurseYRbig = new BitmapFont( Gdx.files.internal( "data/font/curse-y-r-big.fnt" ), Art.fonts.findRegion( "curse-y-r-big" ), true );
		fontCurseGbig = new BitmapFont( Gdx.files.internal( "data/font/curse-g-big.fnt" ), Art.fonts.findRegion( "curse-g-big" ), true );
		fontCurseRbig = new BitmapFont( Gdx.files.internal( "data/font/curse-r-big.fnt" ), Art.fonts.findRegion( "curse-r-big" ), true );

		// friction maps
		frictionNature = new Pixmap( Gdx.files.internal( "data/levels/tilesets/nature/224-friction.png" ) );
	}

	public static void dispose()
	{
		base6[0][0].getTexture().dispose();
		quad.getTexture().dispose();

		carAmbientOcclusion.getTexture().dispose();
		cars.dispose();

		meshMissing.dispose();
		meshTrackWall.dispose();
		meshPalm.dispose();
		meshTribune.dispose();

		// trees
		for(int i = 0; i < 7; i++) meshTreeLeavesSpring[i].dispose();
		meshTreeTrunk.dispose();


		fonts.dispose();

		frictionNature.dispose();

		skidMarksFront.getTexture().dispose();
		skidMarksRear.getTexture().dispose();
	}

	private static TextureRegion[][] split( String name, int width, int height, boolean mipMap )
	{
		return split( name, width, height, false, true, mipMap );
	}

	private static TextureRegion[][] split( String name, int width, int height, boolean flipX, boolean flipY, boolean mipMap )
	{
		Texture texture = newTexture( name, mipMap );
		int xSlices = texture.getWidth() / width;
		int ySlices = texture.getHeight() / height;
		TextureRegion[][] res = new TextureRegion[ xSlices ][ ySlices ];
		for( int x = 0; x < xSlices; x++ )
		{
			for( int y = 0; y < ySlices; y++ )
			{
				res[x][y] = new TextureRegion( texture, x * width, y * height, width, height );
				res[x][y].flip( flipX, flipY );
			}
		}
		return res;
	}

	private static TextureRegion load( String name, int width, int height, boolean mipMap )
	{
		Texture texture = newTexture( name, mipMap );
		TextureRegion region = new TextureRegion( texture, 0, 0, width, height );
		region.flip( false, true );
		return region;
	}

	private static Texture newTexture( String name, boolean mipMap )
	{
		Texture t = new Texture( Gdx.files.internal( name ), Format.RGBA8888, mipMap );

		if(mipMap)
			t.setFilter( TextureFilter.MipMapLinearNearest, TextureFilter.Nearest );
		else
			t.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );

		return t;
	}

	public static void scaleFonts( float scale )
	{
		Art.fontCurseYR.setScale( scale );
		Art.fontCurseG.setScale( scale );
		Art.fontCurseR.setScale( scale );
		Art.fontCurseYRbig.setScale( scale );
		Art.fontCurseGbig.setScale( scale );
		Art.fontCurseRbig.setScale( scale );
	}

}
