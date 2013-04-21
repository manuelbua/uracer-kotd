
package com.bitfire.uracer.game.logic.gametasks.messager;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Back;
import aurelienribon.tweenengine.equations.Expo;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.BitmapFontFactory;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ScaleUtils;

public class Message {
	public enum Type {
		Information, Bad, Good
	}

	/** the position of the displayed message, this also reflects the order in which messages are rendered */
	public enum Position {
		Top, Middle, Bottom
	}

	public enum Size {
		Normal, Big
	}

	public long durationMs;
	public long startMs;
	public boolean started;

	private String what;
	private Position position;
	private float whereX, whereY;
	private float finalY;
	private float scaleX, scaleY;
	private BitmapFont font;
	private int halfWidth;
	private boolean finished;
	private TextBounds bounds;
	private float alpha;
	private boolean hiding;

	public Message () {
		bounds = new TextBounds();
	}

	public Message (String message, float durationSecs, Type type, Position position, Size size) {
		set(message, durationSecs, type, position, size);
	}

	public final void set (String message, float durationSecs, Type type, Position position, Size size) {
		startMs = 0;
		started = false;
		halfWidth = (int)(ScaleUtils.RefScreenWidth / 2);

		what = message;
		this.position = position;
		alpha = 0f;
		scaleX = 1f;
		scaleY = 1f;
		durationMs = (int)(durationSecs * 1000f);
		hiding = false;

		switch (type) {
		case Good:
			if (size == Size.Normal) {
				font = BitmapFontFactory.get(FontFace.CurseGreen);
			} else {
				font = BitmapFontFactory.get(FontFace.CurseGreenBig);
			}
			break;
		case Bad:
			if (size == Size.Normal) {
				font = BitmapFontFactory.get(FontFace.CurseRed);
			} else {
				font = BitmapFontFactory.get(FontFace.CurseRedBig);
			}
			break;
		default:
		case Information:
			if (size == Size.Normal) {
				font = BitmapFontFactory.get(FontFace.CurseRedYellow);
			} else {
				font = BitmapFontFactory.get(FontFace.CurseRedYellowBig);
			}
			break;
		}
	}

	private void computeFinalPosition () {
		int widthOnFour = ScaleUtils.RefScreenWidth / 4;
		whereX = widthOnFour;
		finalY = 0;

		float distance = 180;
		float h = ScaleUtils.RefScreenHeight;

		switch (position) {
		case Top:
			finalY = 30;
			whereY = h / 2;
			break;

		case Middle:
			bounds.set(font.getMultiLineBounds(what));
			finalY = (h - bounds.height) / 2 - bounds.height / 2;
			whereY = h + bounds.height;
			break;

		case Bottom:
			finalY = h - distance;
			whereY = h + distance;
			break;
		}

		font.setScale(1f);
	}

	public boolean tick () {
		return !finished;
	}

	public void render (SpriteBatch batch) {
		font.setScale(scaleX, scaleY);
		font.setColor(1, 1, 1, alpha);
		font.drawMultiLine(batch, what, whereX, whereY, halfWidth, HAlignment.CENTER);
		font.setColor(1, 1, 1, 1);
	}

	public void onShow () {
		finished = false;
		hiding = false;

		// scaleX = scaleY = 1f;
		computeFinalPosition();

		GameTweener.start(Timeline.createParallel().push(Tween.to(this, MessageAccessor.OPACITY, 400).target(1f).ease(Expo.INOUT))
			.push(Tween.to(this, MessageAccessor.POSITION_Y, 400).target(finalY).ease(Expo.INOUT))
			.push(Tween.to(this, MessageAccessor.SCALE_XY, 500).target(1.5f, 1.5f).ease(Back.INOUT)));
	}

	private TweenCallback hideFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				finished = true;
			}
		}
	};

	public void onHide () {
		hiding = true;

		GameTweener.start(Timeline.createParallel().push(Tween.to(this, MessageAccessor.OPACITY, 500).target(0f).ease(Expo.INOUT))
			.push(Tween.to(this, MessageAccessor.POSITION_Y, 500).target(-50 * font.getScaleX()).ease(Expo.INOUT))
			.push(Tween.to(this, MessageAccessor.SCALE_XY, 400).target(1f, 1f).ease(Back.INOUT)).setCallback(hideFinished));
	}

	public boolean isHiding () {
		return hiding;
	}

	public float getX () {
		return whereX;
	}

	public float getY () {
		return whereY;
	}

	public float getScaleX () {
		return scaleX;
	}

	public float getScaleY () {
		return scaleY;
	}

	public float getAlpha () {
		return alpha;
	}

	public void setAlpha (float value) {
		alpha = value;
	}

	public void setPosition (float x, float y) {
		whereX = x;
		whereY = y;
	}

	public void setScale (float scaleX, float scaleY) {
		this.scaleX = AMath.clamp(scaleX, 0.1f, 10f);
		this.scaleY = AMath.clamp(scaleY, 0.1f, 10f);
	}

	public void setX (float x) {
		whereX = x;
	}

	public void setY (float y) {
		whereY = y;
	}
}
