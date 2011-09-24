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
	// Art
	public static TextureRegion[][] base6;
//	public static TextureRegion titleScreen;
	public static TextureRegion quad;
	public static TextureAtlas cars;
//	public static TextureRegion hqCars;
	public static TextureRegion carAmbientOcclusion;

	public static Texture meshMissing;
	public static Texture mesh_test_arch_rusty;
	public static Texture meshPalm;
	public static Texture meshTribune;
	public static Texture meshHouse;
	public static Texture meshTower;

	public static Texture trackWall;
	public static TextureAtlas fonts;

	// fonts
	public static BitmapFont fontCurseYR, fontCurseR, fontCurseG;
	public static BitmapFont fontCurseYRbig, fontCurseRbig, fontCurseGbig;

	public static void load()
	{
//		titleScreen = load( "data/base/titlescreen.png", 480, 320 );
		base6 = split( "data/base/base6.png", 6, 6 );
		quad = new TextureRegion( base6[0][10], 0, 0, 18, 18 );
		cars = new TextureAtlas(Gdx.files.internal("data/base/cars1.pack"));

		mesh_test_arch_rusty = newTexture( "data/3d/test_arch_rusty.jpg" );
		meshMissing = newTexture( "data/3d/missing-mesh.png" );
		meshPalm = newTexture( "data/3d/palm.png" );
		meshTribune = newTexture( "data/3d/tribune.png" );
		meshHouse = newTexture( "data/3d/house.png" );
		meshTower = newTexture( "data/3d/tower.png" );
		trackWall = newTexture( "data/3d/track/wall.jpg" );

		// no mip-mapping
//		hqCars = new TextureRegion( new Texture(Gdx.files.internal("data/base/hqcars.png")), 0, 0, 420, 424 );
		carAmbientOcclusion = new TextureRegion( new Texture(Gdx.files.internal("data/base/car-ao.png")), 0, 0, 34, 62 );

		// fonts
		fonts = new TextureAtlas( "data/base/font/pack" );

		// default size
		fontCurseYR = new BitmapFont( Gdx.files.internal( "data/base/font/curse-y-r.fnt" ), Art.fonts.findRegion( "curse-y-r" ), true );
		fontCurseG = new BitmapFont( Gdx.files.internal( "data/base/font/curse-g.fnt" ), Art.fonts.findRegion( "curse-g" ), true );
		fontCurseR = new BitmapFont( Gdx.files.internal( "data/base/font/curse-r.fnt" ), Art.fonts.findRegion( "curse-r" ), true );

		// big size
		fontCurseYRbig = new BitmapFont( Gdx.files.internal( "data/base/font/curse-y-r-big.fnt" ), Art.fonts.findRegion( "curse-y-r-big" ), true );
		fontCurseGbig = new BitmapFont( Gdx.files.internal( "data/base/font/curse-g-big.fnt" ), Art.fonts.findRegion( "curse-g-big" ), true );
		fontCurseRbig = new BitmapFont( Gdx.files.internal( "data/base/font/curse-r-big.fnt" ), Art.fonts.findRegion( "curse-r-big" ), true );
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
		Texture t = new Texture( Gdx.files.internal( name ), Format.RGB565, Config.EnableMipMapping );

		if(Config.EnableMipMapping)
			t.setFilter( TextureFilter.MipMapLinearNearest, TextureFilter.Nearest );
		else
			t.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );

		return t;
	}

	public static void dispose()
	{
		base6[0][0].getTexture().dispose();
//		titleScreen.getTexture().dispose();
		quad.getTexture().dispose();
//		hqCars.getTexture().dispose();

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
	}
}
