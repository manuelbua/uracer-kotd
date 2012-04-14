package com.bitfire.uracer.game.messager;

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
import com.bitfire.uracer.game.tween.Tweener;
import com.bitfire.uracer.utils.AMath;

public class Message {
	public enum Type {
		Information, Bad, Good
	}

	/** the position of the displayed message, this also reflects the order in which messages are rendered */
	public enum MessagePosition {
		Top, Middle, Bottom
	}

	public enum MessageSize {
		Normal, Big
	}

	public long durationMs;
	public long startMs;
	public boolean started;

	private String what;
	private MessagePosition position;
	private float whereX, whereY;
	private float finalY;
	private float scaleX, scaleY;
	private BitmapFont font;
	private int halfWidth;
	private boolean finished;
	private TextBounds bounds;
	private float alpha;
	private boolean hiding;
	private float invZoomFactor;

	public Message( float invZoomFactor ) {
		bounds = new TextBounds();
		this.invZoomFactor = invZoomFactor;
	}

	public Message( String message, float durationSecs, Type type, MessagePosition position, MessageSize size, float invZoomFactor ) {
		this( invZoomFactor );
		set( message, durationSecs, type, position, size );
	}

	public final void set( String message, float durationSecs, Type type, MessagePosition position, MessageSize size ) {
		startMs = 0;
		started = false;
		halfWidth = (int)(Gdx.graphics.getWidth() / 2);

		what = message;
		this.position = position;
		alpha = 0f;
		scaleX = 1f;
		scaleY = 1f;
		durationMs = (int)(durationSecs * 1000f);
		hiding = false;

		switch( type ) {
		case Good:
			if( size == MessageSize.Normal ) {
				font = Art.fontCurseG;
			} else {
				font = Art.fontCurseGbig;
			}
			break;
		case Bad:
			if( size == MessageSize.Normal ) {
				font = Art.fontCurseR;
			} else {
				font = Art.fontCurseRbig;
			}
			break;
		default:
		case Information:
			if( size == MessageSize.Normal ) {
				font = Art.fontCurseYR;
			} else {
				font = Art.fontCurseYRbig;
			}
			break;
		}
	}

	private void computeFinalPosition() {
		int widthOnFour = Gdx.graphics.getWidth() / 4;
		whereX = widthOnFour;
		finalY = 0;

		float scale = invZoomFactor;
		float distance = 180 * scale;

		switch( position ) {
		case Top:
			finalY = 30 * scale;
			whereY = Gdx.graphics.getHeight() / 2;
			break;

		case Middle:
			font.setScale( 1.5f * scale, 1.5f * scale );
			bounds.set( font.getMultiLineBounds( what ) );
			finalY = (Gdx.graphics.getHeight() - bounds.height) / 2 - bounds.height / 2;
			whereY = Gdx.graphics.getHeight() + bounds.height;
			break;

		case Bottom:
			finalY = Gdx.graphics.getHeight() - distance;
			whereY = Gdx.graphics.getHeight() + distance;
			break;
		}

		font.setScale( scale, scale );
	}

	public boolean tick() {
		return !finished;
	}

	public void render( SpriteBatch batch ) {
		font.setScale( scaleX * invZoomFactor, scaleY * invZoomFactor );
		font.setColor( 1, 1, 1, alpha );
		font.drawMultiLine( batch, what, whereX, whereY, halfWidth, HAlignment.CENTER );
		font.setColor( 1, 1, 1, 1 );
	}

	public void onShow() {
		finished = false;
		hiding = false;

		// scaleX = scaleY = 1f;
		computeFinalPosition();

		Tweener.start( Timeline.createParallel().push( Tween.to( this, MessageAccessor.OPACITY, 400 ).target( 1f ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.POSITION_Y, 400 ).target( finalY ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.SCALE_XY, 500 ).target( 1.5f, 1.5f ).ease( Back.INOUT ) ) );
	}

	private TweenCallback hideFinished = new TweenCallback() {
		@Override
		public void onEvent( int type, BaseTween<?> source ) {
			switch( type ) {
			case COMPLETE:
				finished = true;
			}
		}
	};

	public void onHide() {
		hiding = true;

		Tweener.start( Timeline.createParallel().push( Tween.to( this, MessageAccessor.OPACITY, 500 ).target( 0f ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.POSITION_Y, 500 ).target( -50 * font.getScaleX() ).ease( Expo.INOUT ) )
				.push( Tween.to( this, MessageAccessor.SCALE_XY, 400 ).target( 1f, 1f ).ease( Back.INOUT ) ).setCallback( hideFinished ) );
	}

	public boolean isHiding() {
		return hiding;
	}

	public float getX() {
		return whereX;
	}

	public float getY() {
		return whereY;
	}

	public float getScaleX() {
		return scaleX;
	}

	public float getScaleY() {
		return scaleY;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha( float value ) {
		alpha = value;
	}

	public void setPosition( float x, float y ) {
		whereX = x;
		whereY = y;
	}

	public void setScale( float scaleX, float scaleY ) {
		this.scaleX = AMath.clamp( scaleX, 0.1f, 10f );
		this.scaleY = AMath.clamp( scaleY, 0.1f, 10f );
	}

	public void setX( float x ) {
		whereX = x;
	}

	public void setY( float y ) {
		whereY = y;
	}
}
