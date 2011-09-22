package com.bitfire.uracer.hud;

import java.util.LinkedList;
import java.util.Queue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;

public class Messager
{
	// data
	private static Queue<Message> messages;
	private static Message current;

	// font
	private static BitmapFont font;
	private static SpriteBatch batch;
	private static int halfWidth;
	private static int quarterWidth;
	private static int screenHeight;

	protected Messager()
	{
	}

	public static void init()
	{
		current = null;
		messages = new LinkedList<Message>();
		halfWidth = Gdx.graphics.getWidth() / 2;
		quarterWidth = halfWidth / 2;
		screenHeight = Gdx.graphics.getHeight();

		// setup sprite batch
		batch = new SpriteBatch( 100, 5 );

		// y-flip
		Matrix4 proj = new Matrix4();
		proj.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		batch.setProjectionMatrix( proj );

		// load font
		font = new BitmapFont( Gdx.files.internal( "data/base/font/curse.fnt" ), Art.fonts.findRegion( "curse" ), true );
	}

	public static void dispose()
	{
		font.dispose();
		batch.dispose();
	}

	public static boolean isBusy()
	{
		return (current != null);
	}

	public static void add( String message, float durationSecs )
	{
		Message m = new Message( font, message, durationSecs );
		messages.add( m );
	}

	public static void update()
	{
		// any message?
		if( !isBusy() && (messages.peek() != null) )
		{
			// schedule this message to be processed the next tick
			current = messages.remove();
		} else if( isBusy() )
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

	public static void render()
	{
		if( isBusy() )
		{
			batch.begin();

			font.drawMultiLine( batch, current.what, quarterWidth, screenHeight - current.boundsHeight - 30, halfWidth, HAlignment.CENTER );

			batch.end();
		}
	}

	private static void show( Message m )
	{
		System.out.println( "Showing '" + m.what + "', started at " + m.startMs );
	}

	private static void hide( Message m )
	{
		System.out.println( "Hiding '" + m.what + "', at " + System.currentTimeMillis() + ", after "
				+ (System.currentTimeMillis() - m.startMs) );
	}
}
