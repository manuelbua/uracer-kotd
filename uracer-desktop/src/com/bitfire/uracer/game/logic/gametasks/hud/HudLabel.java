
package com.bitfire.uracer.game.logic.gametasks.hud;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Expo;
import aurelienribon.tweenengine.equations.Quint;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.BitmapFontFactory;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;

public final class HudLabel {
	public float x, y;
	public float alpha;
	public TextBounds bounds = new TextBounds();
	public float halfBoundsWidth, halfBoundsHeight;
	public float boundsWidth, boundsHeight;

	private String what;
	private BitmapFont font;
	private float scale;
	private ScalingStrategy scalingStrategy;
	private boolean isStatic;

	public HudLabel (ScalingStrategy scalingStrategy, FontFace fontFace, String string, boolean isStatic, float scale) {
		this.scalingStrategy = scalingStrategy;
		what = string;
		alpha = 1f;
		this.isStatic = isStatic;
		this.font = BitmapFontFactory.get(fontFace);
		setScale(scale, true);
	}

	public HudLabel (ScalingStrategy scalingStrategy, FontFace fontFace, String string, boolean isStatic) {
		this(scalingStrategy, fontFace, string, isStatic, 1.0f);
	}

	public boolean isVisible () {
		return (alpha > 0);
	}

	// one should avoid rendering artifacts when possible and set this to true
	public void setStatic (boolean isStatic) {
		this.isStatic = isStatic;
	}

	public void setFont (FontFace fontFace) {
		this.font = BitmapFontFactory.get(fontFace);
		recomputeBounds();
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

	public void setPosition (float posX, float posY) {
		x = posX - halfBoundsWidth;
		y = posY - halfBoundsHeight;
	}

	public void setPosition (Vector2 position) {
		x = position.x - halfBoundsWidth;
		y = position.y - halfBoundsHeight;
	}

	private Vector2 tmpos = new Vector2();

	public Vector2 getPosition () {
		tmpos.set(x + halfBoundsWidth, y + halfBoundsHeight);
		return tmpos;
	}

	public void recomputeBounds () {
		font.setScale(scale * scalingStrategy.invTileMapZoomFactor);
		bounds.set(font.getMultiLineBounds(what));
		halfBoundsWidth = bounds.width * 0.5f;
		halfBoundsHeight = bounds.height * 0.5f;
		boundsWidth = bounds.width;
		boundsHeight = bounds.height;
	}

	public TextBounds getBounds () {
		return bounds;
	}

	public float getX () {
		return x + halfBoundsWidth;
	}

	public float getY () {
		return y + halfBoundsHeight;
	}

	public void setX (float v) {
		x = v - halfBoundsWidth;
	}

	public void setY (float v) {
		y = v - halfBoundsHeight;
	}

	public float getAlpha () {
		return alpha;
	}

	public void setAlpha (float value) {
		alpha = value;
	}

	public void setFont (BitmapFont font) {
		this.font = font;
		recomputeBounds();
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

	public void render (SpriteBatch batch) {
		if (alpha > 0) {
			if (isStatic) {
				font.setUseIntegerPositions(true);
			} else {
				font.setUseIntegerPositions(false);
			}

			font.setScale(scale * scalingStrategy.invTileMapZoomFactor);
			font.setColor(1, 1, 1, alpha);

			font.drawMultiLine(batch, what, x, y);

			// font.setColor( 1, 1, 1, 1 );
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

		setPosition(getPosition().x, getPosition().y + 50);
		float targetNearX = getPosition().x;
		float targetNearY = getPosition().y;
		float targetFarX = getPosition().x;
		float targetFarY = getPosition().y - 100;
		if (!slideUp) {
			targetFarY = getPosition().y + 100;
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
