package com.bitfire.uracer.screen;

import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.URacer;

public abstract class Screen
{
	protected static Random random = new Random();
	private URacer uracer;
	public SpriteBatch spriteBatch;

	private float Near = 0f;
	private float Far = 100f;

	public final void init( URacer uracer )
	{
		this.uracer = uracer;
		spriteBatch = new SpriteBatch();

		// y-flip
		Matrix4 proj = new Matrix4();
		proj.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, Near, Far );
		spriteBatch.setProjectionMatrix( proj );
	}

	public void removed()
	{
		spriteBatch.dispose();
	}

	protected void setScreen( Screen screen )
	{
		uracer.setScreen( screen );
	}

	protected String[] chars = { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", ".,!?:;\"'+-=/\\< " };

	public void draw( TextureRegion region, int x, int y )
	{
		int width = region.getRegionWidth();
		if( width < 0 )
			width = -width;

		spriteBatch.draw( region, x, y, width, -region.getRegionHeight() );
	}

	public void drawString( String string, int x, int y )
	{
		string = string.toUpperCase();
		for( int i = 0; i < string.length(); i++ )
		{
			char ch = string.charAt( i );
			for( int ys = 0; ys < chars.length; ys++ )
			{
				int xs = chars[ys].indexOf( ch );
				if( xs >= 0 )
				{
					draw( Art.base6[xs][ys + 9], x + i * 6, y );
				}
			}
		}
	}

	public abstract void render( float timeAliasingFactor );

	public void tick()
	{
	}
}
