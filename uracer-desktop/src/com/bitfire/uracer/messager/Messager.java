package com.bitfire.uracer.messager;

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
	private static Queue<Message> messages;
	private static Message current;

	// font
	private static int halfWidth;

	private Messager()
	{
	}

	public static void init()
	{
		current = null;
		messages = new LinkedList<Message>();
		halfWidth = Gdx.graphics.getWidth() / 2;
	}

	public static void dispose()
	{
		reset();
	}

	public static boolean isBusy()
	{
		return (current != null);
	}

	public static void reset()
	{
		messages.clear();
		current = null;
//		System.out.println("Messages just got cleaned up.");
	}

	public static void tick()
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
				onShow( current );
			}

			// check if finished
			long elapsed = (System.currentTimeMillis() - current.startMs);
			if( elapsed >= current.durationMs )
			{
				// message should end
				onHide( current );
				current = null;
			}
		}
	}

	public static void render( SpriteBatch batch )
	{
		if( isBusy() )
		{
			current.font.setScale( Director.scalingStrategy.invTileMapZoomFactor );
			current.font.drawMultiLine( batch, current.what, current.whereX, current.whereY, halfWidth, HAlignment.CENTER );
		}
	}

	public static void show( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		Message m = new Message( message, durationSecs, type, position, size );
		messages.add( m );
	}

	private static void onShow( Message m )
	{
//		System.out.println( "Showing '" + m.what + "', started at " + m.startMs );
	}

	private static void onHide( Message m )
	{
//		System.out.println( "Hiding '" + m.what + "', at " + System.currentTimeMillis() + ", after "
//				+ (System.currentTimeMillis() - m.startMs) );
	}
}
