package com.bitfire.uracer.messager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;

public class Message
{
	public long durationMs;
	public long startMs;
	public boolean started;

	private String what;
	private MessageType type;
	private MessagePosition position;
	private float whereX, whereY;
	private BitmapFont font;
	private int halfWidth;
	private boolean finished;

	public Message( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		startMs = 0;
		started = false;
		halfWidth = (int)(Gdx.graphics.getWidth() / 2);

		what = message;
		this.type = type;
		this.position = position;
		durationMs = (int)(durationSecs * 1000f);

		switch( this.type )
		{
		default:
		case Information:
			if( size == MessageSize.Normal )
				font = Art.fontCurseYR;
			else
				font = Art.fontCurseYRbig;
			break;

		case Good:
			if( size == MessageSize.Normal )
				font = Art.fontCurseG;
			else
				font = Art.fontCurseGbig;
			break;

		case Bad:
			if( size == MessageSize.Normal )
				font = Art.fontCurseR;
			else
				font = Art.fontCurseRbig;
			break;
		}

		computeFinalPosition();
	}

	private void computeFinalPosition()
	{
		TextBounds bounds = font.getMultiLineBounds( what );

		whereX = Gdx.graphics.getWidth() / 4;
		whereY = 0;

		switch( position )
		{
		case Top:
			whereY = 30 * font.getScaleX();
			break;

		case Middle:
			whereY = (Gdx.graphics.getHeight() - bounds.height) / 2;
			break;

		case Bottom:
			whereY = Gdx.graphics.getHeight() - bounds.height - 30 * font.getScaleX();
			break;
		}

	}

	public boolean tick()
	{
		return !finished;
	}

	public void render( SpriteBatch batch )
	{
		font.drawMultiLine( batch, what, whereX, whereY, halfWidth, HAlignment.CENTER );
	}

	public void onShow()
	{
		finished = false;
	}

	public void onHide()
	{
		finished = true;
	}
}
