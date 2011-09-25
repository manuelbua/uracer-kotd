package com.bitfire.uracer.hud;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;

public class Label
{
	private String what;
	private TextBounds bounds;
	private BitmapFont font;
	private float x, y;

	public Label( BitmapFont font, String string )
	{
		what = string;
		this.font = font;
		bounds = new TextBounds();
		recomputeBounds();
	}

	public void setString( String string )
	{
		what = string;
	}

	public void recomputeBounds()
	{
		bounds.set( font.getMultiLineBounds( what ) );
	}

	public TextBounds getBounds()
	{
		return bounds;
	}

	public void setPosition( float x, float y )
	{
		this.x = x;
		this.y = y;
	}

	public void render( SpriteBatch batch )
	{
		Art.fontCurseYR.drawMultiLine( batch, what, x, y );
	}
}
