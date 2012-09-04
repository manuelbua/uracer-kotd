
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

public final class CarHighlighter {
	private Sprite sprite;
	private Car followedCar;
	private EntityRenderState renderState;
	private Vector2 tmp = new Vector2();
	private float offX, offY;

	private boolean isBusy, isActive;
	private BoxedFloat bfScale, bfRot, bfAlpha, bfGreen, bfRed, bfBlue;

	public CarHighlighter () {
		sprite = new Sprite();
		sprite.setRegion(Art.cars.findRegion("selector"));
		isBusy = false;
		isActive = false;
	}

	public void setCar (Car car) {
		followedCar = car;
		renderState = followedCar.state();

		sprite.setSize(car.getRenderer().getFacet().getWidth() * 1.4f, car.getRenderer().getFacet().getHeight() * 1.4f);
		sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);

		offX = sprite.getOriginX();
		offY = sprite.getOriginY();
		bfScale = new BoxedFloat(1);
		bfRot = new BoxedFloat(0);
		bfAlpha = new BoxedFloat(0);
		bfGreen = new BoxedFloat(1);
		bfRed = new BoxedFloat(1);
		bfBlue = new BoxedFloat(1);
	}

	public void stop () {
		isActive = false;
		isBusy = false;
	}

	public void render (SpriteBatch batch, float cameraZoom) {
		if (isActive && isBusy) {
			tmp.set(GameRenderer.ScreenUtils.worldPxToScreen(renderState.position));

			sprite.setScale(bfScale.value * cameraZoom);
			sprite.setPosition(tmp.x - offX, tmp.y - offY);
			sprite.setRotation(-renderState.orientation + bfRot.value);
			sprite.setColor(bfRed.value, bfGreen.value, bfBlue.value, bfAlpha.value);
			sprite.draw(batch);
		}
	}

	private TweenCallback busyCallback = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				isBusy = false;
			}
		}
	};

	public void error (int blinkCount) {
		if (isBusy) {
			return;
		}

		isBusy = true;
		isActive = true;

		bfScale.value = 1f;
		bfRot.value = 0f;
		bfAlpha.value = 0f;

		bfRed.value = 1f;
		bfGreen.value = 0.1f;
		bfBlue.value = 0f;

		Timeline seq = Timeline.createSequence();

		//@off
		seq
			.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, 100).target(1f).ease(Linear.INOUT))
			.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, 100).target(0f).ease(Linear.INOUT))
			.repeat(blinkCount, 0)
			.setCallback(busyCallback)
		;
		//@on

		GameTweener.start(seq);
	}

	public void track () {
		if (isBusy) {
			return;
		}

		isBusy = true;
		isActive = true;

		bfScale.value = 4f;
		bfRot.value = 90f;
		bfAlpha.value = 0f;

		bfRed.value = 1f;
		bfGreen.value = 1f;
		bfBlue.value = 0f;

		Timeline timeline = Timeline.createParallel();

		//@off
		timeline
			.push(Tween.to(bfScale, BoxedFloatAccessor.VALUE, 500).target(1).ease(Linear.INOUT))
			.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, 500).target(1).ease(Linear.INOUT))
			.push(Tween.to(bfGreen, BoxedFloatAccessor.VALUE, 500).target(0.2f).ease(Linear.INOUT))
			.push(Tween.to(bfRot, BoxedFloatAccessor.VALUE, 500).target(0).ease(Linear.INOUT))
			.pushPause(1000)
			.repeatYoyo(1, 0)
			.setCallback(busyCallback)
			;
		//@on

		GameTweener.start(timeline);
	}
}
