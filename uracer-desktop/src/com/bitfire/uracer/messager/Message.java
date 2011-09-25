package com.bitfire.uracer.messager;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenGroup;
import aurelienribon.tweenengine.equations.Expo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.game.logic.GameLogic;
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
	private TextBounds bounds;
	private float alpha;
	private TweenMessage tweenable;
	private boolean hiding;

	public Message( String message, float durationSecs, MessageType type, MessagePosition position, MessageSize size )
	{
		startMs = 0;
		started = false;
		halfWidth = (int)(Gdx.graphics.getWidth() / 2);

		what = message;
		this.type = type;
		this.position = position;
		bounds = new TextBounds();
		alpha = 1f;
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

		computeFinalPosition();
		tweenable = new TweenMessage( this );
	}

	private void computeFinalPosition()
	{
		bounds.set( font.getMultiLineBounds( what ) );

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
		font.setColor( 1, 1, 1, alpha );
		font.drawMultiLine( batch, what, whereX, whereY, halfWidth, HAlignment.CENTER );
	}

	public void onShow()
	{
//		System.out.println("onShow");
		finished = false;
		hiding = false;
		alpha = 0f;
		GameLogic.getTweener().add( Tween.to( tweenable, TweenMessage.OPACITY, 500, Expo.INOUT ).target( 1f ) );
	}

	public void onHide()
	{
		hiding = true;
//		System.out.println("onHide");
		GameLogic.getTweener().add(
				TweenGroup.sequence(
						Tween.to( tweenable, TweenMessage.OPACITY, 500, Expo.INOUT ).target( 0f ),
						Tween.call( new TweenCallback()
						{
							@Override
							public void tweenEventOccured( Types eventType, Tween tween )
							{
								finished = true;
//								System.out.println("onHide finished");
							}
						} )
				)
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

	public float getOriginX()
	{
		return bounds.width/2;
	}

	public float getOriginY()
	{
		return bounds.height/2;
	}

	public float getScaleX()
	{
		return font.getScaleX();
	}

	public float getScaleY()
	{
		return font.getScaleY();
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
		font.setScale( scaleX, scaleY );
	}
}
