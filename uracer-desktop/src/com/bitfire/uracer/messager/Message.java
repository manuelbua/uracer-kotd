package com.bitfire.uracer.messager;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Back;
import aurelienribon.tweenengine.equations.Expo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.tweener.accessors.MessageAccessor;
import com.bitfire.uracer.utils.AMath;

public class Message
{
	public long durationMs;
	public long startMs;
	public boolean started;

	private String what;
	private MessageType type;
	private MessagePosition position;
	private float whereX, whereY;
	private float finalX, finalY;
	private float scaleX, scaleY;
	private BitmapFont font;
	private int halfWidth;
	private boolean finished;
	private TextBounds bounds;
	private float alpha;
	private boolean hiding;

	public Message()
	{
		bounds = new TextBounds();
	}

	public Message( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		this();
		set( message, durationSecs, type, position, size );
	}

	public void set( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		startMs = 0;
		started = false;
		halfWidth = (int)(Gdx.graphics.getWidth() / 2);

		what = message;
		this.type = type;
		this.position = position;
		alpha = 0f;
		scaleX = scaleY = 1f;
		durationMs = (int)(durationSecs * 1000f);
		hiding = false;

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
	}

	private void computeFinalPosition()
	{
		whereX = finalX = Gdx.graphics.getWidth() / 4;
		finalY = 0;

		float distance = 180 * Director.scalingStrategy.invTileMapZoomFactor;

		switch( position )
		{
		case Top:
			finalY = 30 * Director.scalingStrategy.invTileMapZoomFactor;
			whereY = Gdx.graphics.getHeight() / 2;
			break;

		case Middle:
			font.setScale(1.5f * Director.scalingStrategy.invTileMapZoomFactor, 1.5f * Director.scalingStrategy.invTileMapZoomFactor);
			bounds.set( font.getMultiLineBounds( what ) );
			finalY = (Gdx.graphics.getHeight() - bounds.height) / 2 - bounds.height/2;
			whereY = Gdx.graphics.getHeight() + bounds.height;
			break;

		case Bottom:
			finalY = Gdx.graphics.getHeight() - distance;
			whereY = Gdx.graphics.getHeight() + distance;
			break;
		}

		font.setScale(Director.scalingStrategy.invTileMapZoomFactor,Director.scalingStrategy.invTileMapZoomFactor);
	}

	public boolean tick()
	{
		return !finished;
	}

	public void render( SpriteBatch batch )
	{
		font.setScale(scaleX * Director.scalingStrategy.invTileMapZoomFactor, scaleY * Director.scalingStrategy.invTileMapZoomFactor);
		font.setColor( 1, 1, 1, alpha );
		font.drawMultiLine( batch, what, whereX, whereY, halfWidth, HAlignment.CENTER );
		font.setColor( 1, 1, 1, 1  );
	}

	public void onShow()
	{
		finished = false;
		hiding = false;

//		scaleX = scaleY = 1f;
		computeFinalPosition();

		Game.getTweener().start(
			Timeline.createParallel()
				.push( Tween.to( this, MessageAccessor.OPACITY, 400 ).target( 1f ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.POSITION_Y, 400 ).target( finalY ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.SCALE_XY, 500 ).target( 1.5f, 1.5f ).ease( Back.INOUT ) )
		);
	}

	public void onHide()
	{
		hiding = true;

		Game.getTweener().start(
			Timeline.createParallel()
				.push( Tween.to( this, MessageAccessor.OPACITY, 500 ).target( 0f ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.POSITION_Y, 500 ).target( -50 * font.getScaleX() ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.SCALE_XY, 400 ).target( 1f, 1f ).ease( Back.INOUT ) )
				.addCallback( TweenCallback.EventType.COMPLETE, new TweenCallback()
							{
								@Override
								public void onEvent( EventType eventType, BaseTween source )
								{
									finished = true;
								}
							} )
		);
	}

	public boolean isHiding()
	{
		return hiding;
	}

	public float getX()
	{
		return whereX;
	}

	public float getY()
	{
		return whereY;
	}

	public float getScaleX()
	{
		return scaleX;
	}

	public float getScaleY()
	{
		return scaleY;
	}

	public float getAlpha()
	{
		return alpha;
	}

	public void setAlpha(float value)
	{
		alpha = value;
	}

	public void setPosition(float x, float y)
	{
		whereX = x;
		whereY = y;
	}

	public void setScale(float scaleX, float scaleY)
	{
		this.scaleX = AMath.clamp( scaleX, 0.1f, 10f );
		this.scaleY = AMath.clamp( scaleY, 0.1f, 10f );
	}

	public void setX(float x)
	{
		whereX = x;
	}

	public void setY(float y)
	{
		whereY = y;
	}
}
