package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
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

	public static Texture trackWall;
	public static TextureAtlas fonts;

	// 3d
//	public static TextureAtlas modelsTextures;
	public static Texture meshMissing;
	public static Texture mesh_test_arch_rusty;
	public static Texture meshPalm;
	public static Texture meshTribune;
	public static Texture meshHouse;
	public static Texture meshTower;

	// cars
	public static TextureAtlas carTextures;
	public static TextureAtlas cars;
	public static TextureRegion carAmbientOcclusion;
	public static TextureRegion skidMarksFront, skidMarksRear;

	// fonts
	public static BitmapFont fontCurseYR, fontCurseR, fontCurseG;
	public static BitmapFont fontCurseYRbig, fontCurseRbig, fontCurseGbig;

	public static void load()
	{
		base6 = split( "data/base/base6.png", 6, 6 );
		quad = new TextureRegion( base6[0][10], 0, 0, 18, 18 );

		// 3d models' textures
//		modelsTextures = new TextureAtlas(Gdx.files.internal("data/3d/textures/pack"));

		mesh_test_arch_rusty = newTexture( "data/3d/textures/arch-metal-3.jpg" );
		meshMissing = newTexture( "data/3d/textures/missing-mesh.png" );
		meshPalm = newTexture( "data/3d/textures/palm.png" );
		meshTribune = newTexture( "data/3d/textures/tribune.png" );
		meshHouse = newTexture( "data/3d/textures/house.png" );
		meshTower = newTexture( "data/3d/textures/tower.png" );
		trackWall = newTexture( "data/track/wall.jpg" );

		// no mip-mapping
//		carAmbientOcclusion = new TextureRegion( new Texture(Gdx.files.internal("data/base/car-ao.png")), 0, 0, 34, 62 );

		// cars
		carTextures = new TextureAtlas("data/cars/pack");
		cars = carTextures;
		skidMarksFront = carTextures.findRegion( "skid-marks-front" );	// new TextureRegion(new Texture(Gdx.files.internal( "data/base/skid-marks-front.png")), 0, 0, 34, 62);
		skidMarksRear = carTextures.findRegion( "skid-marks-rear" );	// new TextureRegion(new Texture(Gdx.files.internal( "data/base/skid-marks-rear.png")), 0, 0, 34, 62);
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
	}

	private static TextureRegion[][] split( String name, int width, int height )
	{
		return split( name, width, height, false, true );
	}

	private static TextureRegion[][] split( String name, int width, int height, boolean flipX, boolean flipY )
	{
		Texture texture = newTexture( name );
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

	private static TextureRegion load( String name, int width, int height )
	{
		Texture texture = newTexture( name );
		TextureRegion region = new TextureRegion( texture, 0, 0, width, height );
		region.flip( false, true );
		return region;
	}

	private static Texture newTexture( String name )
	{
		Texture t = new Texture( Gdx.files.internal( name ), Format.RGBA8888, Config.EnableMipMapping );

		if(Config.EnableMipMapping)
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
	public static void dispose()
	{
		base6[0][0].getTexture().dispose();
		quad.getTexture().dispose();

		carAmbientOcclusion.getTexture().dispose();
		cars.dispose();

		meshMissing.dispose();
		mesh_test_arch_rusty.dispose();
		meshPalm.dispose();
		meshTribune.dispose();
		meshHouse.dispose();
		meshTower.dispose();

		trackWall.dispose();
		fonts.dispose();

		skidMarksFront.getTexture().dispose();
		skidMarksRear.getTexture().dispose();
	}
}
