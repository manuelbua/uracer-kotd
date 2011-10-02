package com.bitfire.uracer.hud;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HudLabel
{
	public float x, y;
	public float alpha;

	private String what;
	private TextBounds bounds;
	private BitmapFont font;
	private float scale;

	public HudLabel( BitmapFont font, String string )
	{
		this.font = font;
		bounds = new TextBounds();
		what = string;
		alpha = 1f;
		scale = 1f;
		recomputeBounds();
	}

	public void setString( String string )
	{
		setString( string, false );
	}

	public void setString( String string, boolean computeBounds )
	{
		what = string;
		if( computeBounds ) recomputeBounds();
	}

	public void setPosition( float posX, float posY )
	{
		x = posX;
		y = posY;
	}

	public void recomputeBounds()
	{
		font.setScale( scale );
		bounds.set( font.getMultiLineBounds( what ) );
	}

	public TextBounds getBounds()
	{
		return bounds;
	}

	public float getAlpha()
	{
		return alpha;
	}

	public void setAlpha( float value )
	{
		alpha = value;
	}

	public void setFont( BitmapFont font )
	{
		this.font = font;
		recomputeBounds();
	}

	public void setScale( float scale )
	{
		setScale( scale, false );
	}

	public void setScale( float scale, boolean recomputeBounds )
	{
		this.scale = scale;
		if( recomputeBounds ) recomputeBounds();
	}

	public void render( SpriteBatch batch )
	{
		if( alpha > 0 )
		{
			font.setScale( scale );
			font.setColor( 1, 1, 1, alpha );
			font.drawMultiLine( batch, what, x, y );
			font.setColor( 1, 1, 1, 1 );
		}
	}
}
