package com.bitfire.uracer.hud;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenGroup;
import aurelienribon.tweenengine.equations.Expo;
import aurelienribon.tweenengine.equations.Quint;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.tweenables.TweenHudLabel;

public class HudLabel
{
	public float x, y;
	public float alpha;
	public TextBounds bounds = new TextBounds();
	public float halfBoundsWidth, halfBoundsHeight;

	// need controlled access
	private String what;
	private BitmapFont font;
	private float scale;

	// private
	private TweenHudLabel tween;

	public HudLabel( BitmapFont font, String string, float scale )
	{
		this.font = font;
		this.tween = new TweenHudLabel( this );
		what = string;
		alpha = 1f;
		this.scale = scale;
		this.font.setScale( scale );
		recomputeBounds();
	}

	public HudLabel( BitmapFont font, String string )
	{
		this.font = font;
		this.tween = new TweenHudLabel( this );
		what = string;
		alpha = 1f;
		this.scale = 1f;
		this.font.setScale( scale );
		recomputeBounds();
	}

	public void setString( String string )
	{
		setString( string, false );
	}

	public void setString( String string, boolean computeBounds )
	{
		what = string;
		if( computeBounds ) recomputeBounds();
	}

	public void setPosition( float posX, float posY )
	{
		x = posX - halfBoundsWidth;
		y = posY - halfBoundsHeight;
	}

	private Vector2 tmpos = new Vector2();

	public Vector2 getPosition()
	{
		tmpos.set( x + halfBoundsWidth, y + halfBoundsHeight );
		return tmpos;
	}

	public void recomputeBounds()
	{
		font.setScale( scale );
		bounds.set( font.getMultiLineBounds( what ) );
		halfBoundsWidth = bounds.width * 0.5f;
		halfBoundsHeight = bounds.height * 0.5f;
	}

	public TextBounds getBounds()
	{
		return bounds;
	}

	public float getX() { return x + halfBoundsWidth; }
	public float getY() { return y + halfBoundsHeight; }
	public void setX(float v) { x = v - halfBoundsWidth; }
	public void setY(float v) { y = v - halfBoundsHeight; }

	public float getAlpha()
	{
		return alpha;
	}

	public void setAlpha( float value )
	{
		alpha = value;
	}

	public void setFont( BitmapFont font )
	{
		this.font = font;
		recomputeBounds();
	}

	public float getScale()
	{
		return scale;
	}

	public void setScale( float scale )
	{
		setScale( scale, false );
	}

	public void setScale( float scale, boolean recomputeBounds )
	{
		this.scale = scale;
		if( recomputeBounds ) recomputeBounds();
	}

	public void render( SpriteBatch batch )
	{
		if( alpha > 0 )
		{
			font.setScale( scale );
			font.setColor( 1, 1, 1, alpha );

			font.drawMultiLine( batch, what, x, y );

			font.setColor( 1, 1, 1, 1 );
		}
	}

	/**
	 * effects
	 */

	public void fadeIn( int milliseconds )
	{
		GameLogic.getTweener().add( Tween.to( tween, TweenHudLabel.OPACITY, milliseconds, Expo.INOUT ).target( 1f ) );
	}

	public void fadeOut( int milliseconds )
	{
		GameLogic.getTweener().add( Tween.to( tween, TweenHudLabel.OPACITY, milliseconds, Expo.INOUT ).target( 0f ) );
	}

	public void fadeInFor( int milliseconds, int showDurationMs )
	{
		GameLogic.getTweener().add
		(
			TweenGroup.sequence
			(
				Tween.to( tween, TweenHudLabel.OPACITY, milliseconds, Expo.INOUT ).target( 1f ),
				Tween.to( tween, TweenHudLabel.OPACITY, milliseconds, Expo.INOUT ).target( 0f ).delay( showDurationMs )
			)
		);
	}


//	private Vector2 tmpv = new Vector2();
	public void slide( Vector2 heading, float step )
	{
		setScale( 1f, true );

		float targetNearX = getPosition().x;
		float targetNearY = getPosition().y - 30;
		float targetFarX = getPosition().x;
		float targetFarY = getPosition().y - 100;

		GameLogic.getTweener().add
		(
			TweenGroup.parallel
			(
				Tween.to( tween, TweenHudLabel.OPACITY, 500, Quint.INOUT ).target( 1f ),
				TweenGroup.sequence
				(
					Tween.to( tween, TweenHudLabel.POSITION_XY, 500, Quint.INOUT ).target( targetNearX, targetNearY ),
					TweenGroup.parallel
					(
						Tween.to( tween, TweenHudLabel.POSITION_XY, 500, Expo.OUT ).target( targetFarX, targetFarY ),
						Tween.to( tween, TweenHudLabel.OPACITY, 500, Expo.OUT ).target( 0f )
					).delay( 200 )
				)
			)
		);
	}
}
