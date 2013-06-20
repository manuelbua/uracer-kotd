
package com.bitfire.uracer.game.logic.gametasks.hud;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Expo;
import aurelienribon.tweenengine.equations.Linear;
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

	private String text;
	private BitmapFont font;
	private boolean isStatic;
	private Color color = new Color(Color.WHITE);

	public HudLabel (FontFace fontFace, String text, boolean isStatic) {
		this.text = text;
		alpha = 1f;
		this.isStatic = isStatic;
		this.font = BitmapFontFactory.get(fontFace);
		setScale(1);
	}

	public boolean isVisible () {
		return (alpha > 0);
	}

	public void setStatic (boolean isStatic) {
		this.isStatic = isStatic;
	}

	public void setFont (FontFace fontFace) {
		this.font = BitmapFontFactory.get(fontFace);
		updateBounds();
	}

	public void setColor (Color color) {
		this.color.set(color);
	}

	public void setColor (float r, float g, float b) {
		this.color.set(r, g, b, 0);
	}

	public void setString (String string) {
		setString(string, false);
	}

	public void setString (String string, boolean computeBounds) {
		text = string;
		if (computeBounds) {
			updateBounds();
		}
	}

	private void updateBounds () {
		font.setScale(scale);
		textBounds.set(font.getMultiLineBounds(text));
	}

	@Override
	public float getWidth () {
		updateBounds();
		return textBounds.width;
	}

	@Override
	public float getHeight () {
		updateBounds();
		return textBounds.height;
	}

	public float getAlpha () {
		return alpha;
	}

	public void setAlpha (float value) {
		alpha = value;
	}

	public void render (SpriteBatch batch) {
		if (alpha > 0) {
			font.setUseIntegerPositions(isStatic);
			font.setScale(scale);
			font.setColor(color.r, color.g, color.b, alpha);
			updateBounds();
			font.drawMultiLine(batch, text, position.x - textBounds.width / 2, position.y - textBounds.height / 2);
		}
	}

	/** effects */

	public void fadeIn (int milliseconds) {
		GameTweener.stop(this);
		GameTweener.start(Timeline.createSequence().push(
			Tween.to(this, HudLabelAccessor.OPACITY, milliseconds).target(1f).ease(Linear.INOUT)));
		// Gdx.app.log("", "fadein");
	}

	public void fadeOut (int milliseconds) {
		GameTweener.stop(this);
		GameTweener.start(Timeline.createSequence().push(
			Tween.to(this, HudLabelAccessor.OPACITY, milliseconds).target(0f).ease(Linear.INOUT)));
		// Gdx.app.log("", "fadeout");
	}

	public void slide (boolean slideUp) {
		setScale(1);

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
