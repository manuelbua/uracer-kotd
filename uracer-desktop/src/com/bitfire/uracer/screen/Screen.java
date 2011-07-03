package com.bitfire.uracer.screen;

import java.util.Random;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;

public abstract class Screen
{
	protected static Random random = new Random();
	private URacer uracer;
	public SpriteBatch spriteBatch;

	public final void init( URacer uracer )
	{
		this.uracer = uracer;
		spriteBatch = new SpriteBatch( 100 );
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
					// draw( Art.guys[xs][ys + 9], x + i * 6, y );
				}
			}
		}
	}

	public abstract void render(float timeAliasingFactor);

	public void tick( Input input )
	{
	}

}
