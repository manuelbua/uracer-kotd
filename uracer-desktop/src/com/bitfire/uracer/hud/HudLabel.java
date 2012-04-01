package com.bitfire.uracer.hud;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Expo;
import aurelienribon.tweenengine.equations.Quint;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.tweener.accessors.HudLabelAccessor;

public class HudLabel {
	public float x, y;
	public float alpha;
	public TextBounds bounds = new TextBounds();
	public float halfBoundsWidth, halfBoundsHeight;

	// need controlled access
	private String what;
	private BitmapFont font;
	private float scale;

	public HudLabel( BitmapFont font, String string, float scale ) {
		this.font = font;
		what = string;
		alpha = 1f;
		setScale( scale, true );
	}

	public HudLabel( BitmapFont font, String string ) {
		this.font = font;
		what = string;
		alpha = 1f;
		setScale( 1.0f, true );
	}

	public void setString( String string ) {
		setString( string, false );
	}

	public void setString( String string, boolean computeBounds ) {
		what = string;
		if( computeBounds )
			recomputeBounds();
	}

	public void setPosition( float posX, float posY ) {
		x = posX - halfBoundsWidth;
		y = posY - halfBoundsHeight;
	}

	private Vector2 tmpos = new Vector2();

	public Vector2 getPosition() {
		tmpos.set( x + halfBoundsWidth, y + halfBoundsHeight );
		return tmpos;
	}

	public void recomputeBounds() {
		font.setScale( scale );
		bounds.set( font.getMultiLineBounds( what ) );
		halfBoundsWidth = bounds.width * 0.5f;
		halfBoundsHeight = bounds.height * 0.5f;
	}

	public TextBounds getBounds() {
		return bounds;
	}

	public float getX() {
		return x + halfBoundsWidth;
	}

	public float getY() {
		return y + halfBoundsHeight;
	}

	public void setX( float v ) {
		x = v - halfBoundsWidth;
	}

	public void setY( float v ) {
		y = v - halfBoundsHeight;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha( float value ) {
		alpha = value;
	}

	public void setFont( BitmapFont font ) {
		this.font = font;
		recomputeBounds();
	}

	public float getScale() {
		return scale;
	}

	public void setScale( float scale ) {
		setScale( scale, false );
	}

	public void setScale( float scale, boolean recomputeBounds ) {
		this.scale = scale;
		if( recomputeBounds )
			recomputeBounds();
	}

	public void render( SpriteBatch batch ) {
		if( alpha > 0 ) {
			font.setScale( scale * GameData.scalingStrategy.invTileMapZoomFactor );
			font.setColor( 1, 1, 1, alpha );

			font.drawMultiLine( batch, what, x, y );

			// font.setColor( 1, 1, 1, 1 );
		}
	}

	/** effects */

	public void fadeIn( int milliseconds ) {
		GameData.tweener.start( Timeline.createSequence().push( Tween.to( this, HudLabelAccessor.OPACITY, milliseconds ).target( 1f ).ease( Expo.INOUT ) ) );
	}

	public void fadeOut( int milliseconds ) {
		GameData.tweener.start( Timeline.createSequence().push( Tween.to( this, HudLabelAccessor.OPACITY, milliseconds ).target( 0f ).ease( Expo.INOUT ) ) );
	}

	public void slide() {
		setScale( 1f, true );

		setPosition( getPosition().x, getPosition().y + 50 );
		float targetNearX = getPosition().x;
		float targetNearY = getPosition().y;
		float targetFarX = getPosition().x;
		float targetFarY = getPosition().y - 100;

		GameData.tweener.start( Timeline
				.createParallel()
				.push( Tween.to( this, HudLabelAccessor.OPACITY, 500 ).target( 1f ).ease( Quint.INOUT ) )
				.push( Timeline
						.createSequence()
						.push( Tween.to( this, HudLabelAccessor.POSITION_XY, 500 ).target( targetNearX, targetNearY ).ease( Quint.INOUT ).delay( 300 ) )
						.push( Timeline.createParallel().push( Tween.to( this, HudLabelAccessor.POSITION_XY, 500 ).target( targetFarX, targetFarY ).ease( Expo.OUT ) )
								.push( Tween.to( this, HudLabelAccessor.OPACITY, 500 ).target( 0f ).ease( Expo.OUT ) ) ) ) );
	}
}
