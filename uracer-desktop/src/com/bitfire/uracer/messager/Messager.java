package com.bitfire.uracer.messager;

import java.util.LinkedList;
import java.util.Queue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

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

	private Messager()
	{
	}

	public static void init()
	{
		current = null;
		messages = new LinkedList<Message>();
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
				current.onShow();
			}

			if( !current.tick() )
			{
				current = null;
				return;
			}

			// check if finished
			if( (System.currentTimeMillis() - current.startMs) >= current.durationMs && !current.isHiding())
			{
				// message should end
				current.onHide();
			}
		}
	}

	public static void render( SpriteBatch batch )
	{
		if( isBusy() )
		{
			current.render( batch );
		}
	}

	public static void show( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		if(isBusy()) current.onHide();
		enqueue( message, durationSecs, type, position, size );
	}

	public static void enqueue( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		Message m = new Message( message, durationSecs, type, position, size );
		messages.add( m );
	}
}
