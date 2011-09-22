package com.bitfire.uracer.hud;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

public class Message
{
	public String what;
	public long durationMs;
	public long startMs;
	public boolean started;
	public float boundsWidth, boundsHeight;

	public Message( BitmapFont font, String message, float durationSecs )
	{
		startMs = 0;
		started = false;

		what = message;
		durationMs = (int)(durationSecs * 1000f);

		// compute final position
		TextBounds bounds = font.getMultiLineBounds( what );
		boundsWidth = bounds.width;
		boundsHeight = bounds.height;
	}
}
