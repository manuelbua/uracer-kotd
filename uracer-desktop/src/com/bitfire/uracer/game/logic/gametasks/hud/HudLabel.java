
package com.bitfire.uracer.game.logic.gametasks.hud;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Expo;
import aurelienribon.tweenengine.equations.Quint;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.BitmapFontFactory;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;

public final class HudLabel extends Positionable {
	public float alpha;
	public TextBounds textBounds = new TextBounds();

	private String what;
	private BitmapFont font;
	private float scale;
	private final float invTileZoom;;
	private boolean isStatic;
	private FontFace fontFace; // cached
	private Color color = new Color(Color.WHITE);

	// show queue logic
	private int showSemaphore;
	private boolean showing;
	private int qInMs, qOutMs;

	public HudLabel (float invTilemapZoomFactor, FontFace fontFace, String string, boolean isStatic, float scale) {
		this.invTileZoom = invTilemapZoomFactor;
		what = string;
		alpha = 1f;
		this.isStatic = isStatic;
		this.font = BitmapFontFactory.get(fontFace);
		setScale(scale, true);

		showSemaphore = 0;
		showing = false;
	}

	public HudLabel (float invTilemapZoomFactor, FontFace fontFace, String string, boolean isStatic) {
		this(invTilemapZoomFactor, fontFace, string, isStatic, 1.0f);
	}

	public boolean isVisible () {
		return (alpha > 0);
	}

	// one should avoid rendering artifacts when possible and set this to true
	public void setStatic (boolean isStatic) {
		this.isStatic = isStatic;
	}

	public void setFont (FontFace fontFace) {
		this.fontFace = fontFace;
		this.font = BitmapFontFactory.get(fontFace);
		recomputeBounds();
	}

	public void setColor (Color color) {
		this.color.set(color);
	}

	public void setColor (float r, float g, float b) {
		this.color.set(r, g, b, 0);
	}

	public FontFace getFont () {
		return fontFace;
	}

	public void setString (String string) {
		setString(string, false);
	}

	public void setString (String string, boolean computeBounds) {
		what = string;
		if (computeBounds) {
			recomputeBounds();
		}
	}

	public void recomputeBounds () {
		font.setScale(scale * invTileZoom);
		textBounds.set(font.getMultiLineBounds(what));
		bounds.set(textBounds.width, textBounds.height);
		halfBounds.set(textBounds.width * 0.5f, textBounds.height * 0.5f);
	}

	public TextBounds getTextBounds () {
		return textBounds;
	}

	public float getAlpha () {
		return alpha;
	}

	public void setAlpha (float value) {
		alpha = value;
	}

	public float getScale () {
		return scale;
	}

	public void setScale (float scale) {
		setScale(scale, true);
	}

	public void setScale (float scale, boolean recomputeBounds) {
		this.scale = scale;
		if (recomputeBounds) {
			recomputeBounds();
		}
	}

	/** Performs show-queue logic */
	public void tick () {
		if (showSemaphore > 0 && !showing) {
			showing = true;
			fadeIn(qInMs);
		} else if (showSemaphore == 0 && showing) {
			showing = false;
			fadeOut(qOutMs);
		}
	}

	public void render (SpriteBatch batch) {
		if (alpha > 0) {
			if (isStatic) {
				font.setUseIntegerPositions(true);
			} else {
				font.setUseIntegerPositions(false);
			}

			font.setScale(scale * invTileZoom);
			font.setColor(color.r, color.g, color.b, alpha);

			font.drawMultiLine(batch, what, position.x - halfBounds.x, position.y - halfBounds.y);

			// font.setColor( 1, 1, 1, 1 );
		}
	}

	/** queue operations */
	public void queueShow (int millis) {
		showSemaphore++;
		qInMs = millis;
	}

	public void queueHide (int millis) {
		showSemaphore--;
		qOutMs = millis;
		if (showSemaphore < 0) {
			showSemaphore = 0;
		}
	}

	/** effects */

	public void fadeIn (int milliseconds) {
		GameTweener.start(Timeline.createSequence().push(
			Tween.to(this, HudLabelAccessor.OPACITY, milliseconds).target(1f).ease(Expo.INOUT)));
	}

	public void fadeOut (int milliseconds) {
		GameTweener.start(Timeline.createSequence().push(
			Tween.to(this, HudLabelAccessor.OPACITY, milliseconds).target(0f).ease(Expo.INOUT)));
	}

	public void slide (boolean slideUp) {
		setScale(1f, true);

		position.y += 50;
		float targetNearX = position.x;
		float targetNearY = position.y;
		float targetFarX = position.x;
		float targetFarY = position.y - 100;
		if (!slideUp) {
			targetFarY = position.y + 100;
		}

		GameTweener
			.start(Timeline
				.createParallel()
				.push(Tween.to(this, HudLabelAccessor.OPACITY, 500).target(1f).ease(Quint.INOUT))
				.push(
					Timeline
						.createSequence()
						.push(
							Tween.to(this, HudLabelAccessor.POSITION_XY, 500).target(targetNearX, targetNearY).ease(Quint.INOUT)
								.delay(300))
						.push(
							Timeline.createParallel()
								.push(Tween.to(this, HudLabelAccessor.POSITION_XY, 500).target(targetFarX, targetFarY).ease(Expo.OUT))
								.push(Tween.to(this, HudLabelAccessor.OPACITY, 500).target(0f).ease(Expo.OUT)))));
	}
}
