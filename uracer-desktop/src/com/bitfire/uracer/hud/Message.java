package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.hud.Messager.MessagePosition;
import com.bitfire.uracer.hud.Messager.MessageSize;
import com.bitfire.uracer.hud.Messager.MessageType;

public class Message
{
	public String what;
	public long durationMs;
	public long startMs;
	public boolean started;
	public float boundsWidth, boundsHeight;
	public MessageType type;
	public MessagePosition position;
	public float whereX, whereY;
	public BitmapFont font;

	public Message( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		startMs = 0;
		started = false;

		what = message;
		this.type = type;
		this.position = position;
		durationMs = (int)(durationSecs * 1000f);

		switch( type )
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
		boundsWidth = bounds.width;
		boundsHeight = bounds.height;

		whereX = Gdx.graphics.getWidth() / 4;
		whereY = 0;

		switch( position )
		{
		case Top:
			whereY = 30 * font.getScaleX();
			break;

		case Middle:
			whereY = (Gdx.graphics.getHeight() - boundsHeight ) / 2;
			break;

		case Bottom:
			whereY = Gdx.graphics.getHeight() - boundsHeight - 30 * font.getScaleX();
			break;
		}

	}
}
