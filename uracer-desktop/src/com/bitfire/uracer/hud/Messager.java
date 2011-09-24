package com.bitfire.uracer.hud;

import java.util.LinkedList;
import java.util.Queue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Director;

public class Messager
{
	public enum MessageType
	{
		Information, Bad, Good
	}

	public enum MessagePosition
	{
		Top, Middle, Bottom
	}

	public enum MessageSize
	{
		Normal, Big
	}

	// data
	private Queue<Message> messages;
	private Message current;

	// font
	private int halfWidth;

	protected Messager()
	{
		current = null;
		messages = new LinkedList<Message>();
		halfWidth = Gdx.graphics.getWidth() / 2;
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

	public void add( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		Message m = new Message( message, durationSecs, type, position, size );
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

	public void render( SpriteBatch batch )
	{
		if( isBusy() )
		{
			current.font.setScale( Director.scalingStrategy.invTileMapZoomFactor );
			current.font.drawMultiLine( batch, current.what, current.whereX, current.whereY, halfWidth, HAlignment.CENTER );
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
