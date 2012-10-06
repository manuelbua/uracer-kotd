
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.Convert;

public class WrongWay {
	private float w, h, x, y;
	private Sprite sign;
	private BoxedFloat bfAlpha;

	public WrongWay () {
		sign = new Sprite(Art.wrongWay);
		float ratio = Art.wrongWay.getWidth() / Art.wrongWay.getHeight();
		float scale = 0.8f;
		w = Convert.scaledPixels(Art.wrongWay.getWidth()) * scale;
		h = Convert.scaledPixels(Art.wrongWay.getHeight()) * scale;
		x = (Gdx.graphics.getWidth() - w) / 2;
		y = (Gdx.graphics.getHeight() - h) / 2;
		sign.setSize(w, h);
		sign.setPosition(x, y);
		sign.setOrigin(sign.getWidth() / 2, sign.getHeight() / 2);
		sign.flip(false, true);
		bfAlpha = new BoxedFloat(0);
	}

	public void fadeIn () {
		fadeIn(500);
	}

	public void fadeOut () {
		fadeOut(500);
	}

	public void fadeIn (int millisecs) {
		Timeline seq = Timeline.createSequence();
		seq.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, millisecs).target(1f).ease(Linear.INOUT));
		GameTweener.start(seq);
	}

	public void fadeOut (int millisecs) {
		Timeline seq = Timeline.createSequence();
		seq.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, millisecs).target(0f).ease(Linear.INOUT));
		GameTweener.start(seq);
	}

	public void render (SpriteBatch batch) {
		if (!AMath.isZero(bfAlpha.value)) {
			sign.draw(batch, bfAlpha.value);
		}
	}
}
