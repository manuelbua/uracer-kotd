package com.bitfire.uracer.hud;

import java.util.LinkedList;
import java.util.Queue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;

public class Messager
{
	// data
	private Queue<Message> messages;
	private Message current;

	// font
	private BitmapFont font;
	private int halfWidth;
	private int quarterWidth;
	private int screenHeight;

	protected Messager()
	{
		current = null;
		messages = new LinkedList<Message>();
		halfWidth = Gdx.graphics.getWidth() / 2;
		quarterWidth = halfWidth / 2;
		screenHeight = Gdx.graphics.getHeight();

		// load font
		font = Art.fontCurse;
		font.setScale( Director.scalingStrategy.invTileMapZoomFactor );
	}

	public void dispose()
	{
		reset();
	}

	public boolean isBusy()
	{
		return (current != null);
	}

	public void reset()
	{
		messages.clear();
		current = null;
	}

	public void add( String message, float durationSecs )
	{
		Message m = new Message( font, message, durationSecs );
		messages.add( m );
	}

	public void update()
	{
		// any message?
		if( !isBusy() && (messages.peek() != null) )
		{
			// schedule this message to be processed next
			current = messages.remove();
		}

		// busy or became busy?
		if( isBusy() )
		{
			// start message if needed
			if( !current.started )
			{
				current.started = true;
				current.startMs = System.currentTimeMillis();
				show( current );
			}

			// check if finished
			long elapsed = (System.currentTimeMillis() - current.startMs);
			if( elapsed >= current.durationMs )
			{
				// message should end
				hide( current );
				current = null;
			}
		}
	}

	public void render(SpriteBatch batch)
	{
		if( isBusy() )
		{
			font.drawMultiLine( batch, current.what, quarterWidth, screenHeight - current.boundsHeight - 30 * font.getScaleX(),
					halfWidth, HAlignment.CENTER );
		}
	}

	private void show( Message m )
	{
		System.out.println( "Showing '" + m.what + "', started at " + m.startMs );
	}

	private void hide( Message m )
	{
		System.out.println( "Hiding '" + m.what + "', at " + System.currentTimeMillis() + ", after "
				+ (System.currentTimeMillis() - m.startMs) );
	}
}
