package com.bitfire.uracer.messager;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenGroup;
import aurelienribon.tweenengine.equations.Back;
import aurelienribon.tweenengine.equations.Expo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.tweenables.TweenMessage;
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

		tweenable = new TweenMessage( this );
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

		GameLogic.getTweener().add(
				TweenGroup.parallel(
						Tween.to( tweenable, TweenMessage.OPACITY, 400, Expo.INOUT ).target( 1f ),
						Tween.to( tweenable, TweenMessage.POSITION_Y, 400, Expo.INOUT ).target( finalY ),
						Tween.to( tweenable, TweenMessage.SCALE_XY, 500, Back.INOUT ).target( 1.5f, 1.5f )
				)
		);
	}

	public void onHide()
	{
		hiding = true;
		GameLogic.getTweener().add(
				TweenGroup.sequence(
						TweenGroup.parallel(
								Tween.to( tweenable, TweenMessage.OPACITY, 500, Expo.INOUT ).target( 0f ),
								Tween.to( tweenable, TweenMessage.POSITION_Y, 500, Expo.INOUT ).target( -50 * font.getScaleX() ),
								Tween.to( tweenable, TweenMessage.SCALE_XY, 400, Back.INOUT ).target( 1f, 1f )
						),
						Tween.call( new TweenCallback()
						{
							@Override
							public void tweenEventOccured( Types eventType, Tween tween )
							{
								finished = true;
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
