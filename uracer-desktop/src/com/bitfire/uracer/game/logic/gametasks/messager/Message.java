
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
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.resources.BitmapFontFactory;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;

public final class Message {
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
	private float whereX, whereY, startY;
	private float finalY;
	private float scaleX, scaleY;
	private BitmapFont font;
	private int halfWidth;
	private boolean completed;
	private TextBounds bounds;
	private float alpha, scale;
	private boolean hiding;
	private boolean showCompleted;

	public Message () {
		bounds = new TextBounds();
	}

	public Message (String message, float durationSecs, Type type, Position position, Size size) {
		set(message, durationSecs, type, position, size);
	}

	public final void set (String message, float durationSecs, Type type, Position position, Size size) {
		startMs = 0;
		started = false;
		halfWidth = (int)(Config.Graphics.ReferenceScreenWidth / 2);

		what = message;
		this.position = position;
		alpha = 0f;
		scaleX = 1f;
		scaleY = 1f;
		scale = 1.5f;
		durationMs = (long)(durationSecs * 1000f);
		hiding = false;
		completed = false;
		showCompleted = false;

		SysTweener.stop(this);

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
				// font = BitmapFontFactory.get(FontFace.CurseRedYellowNew);
			}
			break;
		}

		// if (size == Size.Big) {
		// scale = 2.5f;
		// }
	}

	private void computeFinalPosition () {
		int widthOnFour = Config.Graphics.ReferenceScreenWidth / 4;
		whereX = widthOnFour;
		startY = finalY = 0;

		float h = Config.Graphics.ReferenceScreenHeight;
		font.setScale(scale);
		bounds.set(font.getMultiLineBounds(what));

		switch (position) {
		case Top:
			startY = whereY = -(font.getLineHeight() - bounds.height);
			finalY = 10;
			break;

		case Middle:
			startY = whereY = (h - bounds.height) / 2;
			finalY = startY - bounds.height / 2;
			break;

		case Bottom:
			startY = whereY = h;
			finalY = h - font.getLineHeight() - 70;
			break;
		}

		// Gdx.app.log(position.toString(), "s=" + startY + ", e=" + finalY);
	}

	public void render (SpriteBatch batch) {
		font.setScale(scaleX, scaleY);
		font.setColor(1, 1, 1, alpha);
		font.drawMultiLine(batch, what, whereX, whereY, halfWidth, HAlignment.CENTER);
	}

	private TweenCallback showFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				showCompleted = true;
			}
		}
	};

	public void show () {
		completed = false;
		hiding = false;

		setAlpha(0);
		setScale(0, 0);
		showCompleted = false;

		computeFinalPosition();

		SysTweener.stop(this);

		//@off
		SysTweener.start(Timeline.createParallel()
			.push(Tween.to(this, MessageAccessor.OPACITY, 850).target(1f).ease(Expo.INOUT))
			.push(Tween.to(this, MessageAccessor.POSITION_Y, 700).target(finalY).ease(Expo.INOUT))
			.push(Tween.to(this, MessageAccessor.SCALE_XY, 800).target(scale, scale).ease(Back.INOUT)).setCallback(showFinished));
		//@on
	}

	private TweenCallback hideFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				completed = true;
			}
		}
	};

	public void hide () {
		if (!hiding) {
			hiding = true;

			SysTweener.stop(this);

			//@off
			SysTweener.start(Timeline.createParallel()
				.push(Tween.to(this, MessageAccessor.OPACITY, 600).target(0f).ease(Expo.INOUT))
				.push(Tween.to(this, MessageAccessor.POSITION_Y, 700).target(startY).ease(Expo.INOUT))
				.push(Tween.to(this, MessageAccessor.SCALE_XY, 800).target(0, 0).ease(Back.INOUT)).setCallback(hideFinished));
			//@on
		}
	}

	public boolean isShowComplete () {
		return showCompleted;
	}

	public boolean isCompleted () {
		return completed;
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
