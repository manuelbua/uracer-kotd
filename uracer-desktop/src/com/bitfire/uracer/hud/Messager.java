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
	public enum MessageType
	{
		Information, Bad, Good
	}

	public enum MessagePosition
	{
		Top, Middle, Bottom
	}

	// data
	private Queue<Message> messages;
	private Message current;

	// font
	private BitmapFont font;
	private int halfWidth;

	protected Messager()
	{
		current = null;
		messages = new LinkedList<Message>();
		halfWidth = Gdx.graphics.getWidth() / 2;

		// default font
		font = Art.fontCurseYR;
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
		add( message, durationSecs, MessageType.Information, MessagePosition.Bottom );
	}

	public void add( String message, float durationSecs, MessageType type )
	{
		Message m = new Message( font, message, durationSecs, type, MessagePosition.Bottom );
		messages.add( m );
	}

	public void add( String message, float durationSecs, MessagePosition position )
	{
		Message m = new Message( font, message, durationSecs, MessageType.Information, MessagePosition.Bottom );
		messages.add( m );
	}

	public void add( String message, float durationSecs, MessageType type, MessagePosition position )
	{
		Message m = new Message( font, message, durationSecs, type, position );
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
			switch( current.type )
			{
			default:
			case Information:
				font = Art.fontCurseYR;
				break;

			case Good:
				font = Art.fontCurseG;
				break;

			case Bad:
				font = Art.fontCurseR;
				break;
			}

			font.setScale( Director.scalingStrategy.invTileMapZoomFactor );
			font.drawMultiLine( batch, current.what, current.whereX, current.whereY, halfWidth, HAlignment.CENTER );
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
