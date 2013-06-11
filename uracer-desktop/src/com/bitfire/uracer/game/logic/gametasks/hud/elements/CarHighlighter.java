
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.Convert;

public final class CarHighlighter {
	private Sprite sprite;
	private Car followedCar;
	private EntityRenderState renderState;
	private Vector2 tmp = new Vector2();
	private float offX, offY, alpha, scale;

	private boolean isBusy, isActive, hasCar;
	private BoxedFloat bfScale, bfRot, bfAlpha, bfGreen, bfRed, bfBlue;

	// need tileMapZoomFactor since highlighter size depends from car *rendered* size
	public CarHighlighter () {
		sprite = new Sprite();
		sprite.setRegion(Art.cars.findRegion("selector"));
		isBusy = false;
		isActive = false;
		followedCar = null;
		alpha = 1;
	}

	public void setCar (Car car) {
		hasCar = true;
		followedCar = car;
		renderState = followedCar.state();

		CarModel model = car.getCarModel();
		sprite.setSize(Convert.mt2px(model.width) * 1.4f, Convert.mt2px(model.length) * 1.4f);
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

	public void setAlpha (float alpha) {
		this.alpha = alpha;
	}

	public void setScale (float scale) {
		this.scale = scale;
	}

	public Car getCar () {
		return followedCar;
	}

	public void stop () {
		isActive = false;
		isBusy = false;
	}

	public void render (SpriteBatch batch, float cameraZoom) {
		if (isActive && hasCar) {
			tmp.set(GameRenderer.ScreenUtils.worldPxToScreen(renderState.position));

			float timeFactor = URacer.Game.getTimeModFactor() * 0.3f;
			float s = 1f + timeFactor;

			sprite.setScale(bfScale.value * cameraZoom * scale * s);
			sprite.setPosition(tmp.x - offX, tmp.y - offY);
			sprite.setRotation(-renderState.orientation + bfRot.value);
			sprite.setColor(bfRed.value, bfGreen.value, bfBlue.value, bfAlpha.value);
			sprite.draw(batch, alpha);
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

		GameTweener.stop(bfAlpha);

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
		// if (isBusy) {
		// return;
		// }

		isBusy = true;
		isActive = true;

		bfScale.value = 4f;
		bfAlpha.value = 0f;
		bfRot.value = -90f;

		bfRed.value = 1f;
		bfGreen.value = 1f;
		bfBlue.value = 1f;

		Timeline timeline = Timeline.createParallel();
		float ms = Config.Graphics.DefaultFadeMilliseconds;

		GameTweener.stop(bfAlpha);
		GameTweener.stop(bfScale);
		GameTweener.stop(bfRot);

		//@off
		timeline
			.push(Tween.to(bfScale, BoxedFloatAccessor.VALUE, ms).target(1f).ease(Linear.INOUT))
			.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, ms).target(1f).ease(Linear.INOUT))
			.push(Tween.to(bfRot, BoxedFloatAccessor.VALUE, ms).target(0f).ease(Linear.INOUT))
			.setCallback(busyCallback)
		;
		//@on

		GameTweener.start(timeline);
	}

	public void untrack () {
		// if (isBusy) {
		// return;
		// }

		isBusy = true;
		isActive = true;

		bfScale.value = 1f;
		bfAlpha.value = 1f;
		bfRot.value = 0f;

		bfRed.value = 1f;
		bfGreen.value = 1f;
		bfBlue.value = 1f;

		Timeline timeline = Timeline.createParallel();
		float ms = Config.Graphics.DefaultFadeMilliseconds;

		GameTweener.stop(bfAlpha);
		GameTweener.stop(bfScale);
		GameTweener.stop(bfRot);

		//@off
		timeline
			.push(Tween.to(bfScale, BoxedFloatAccessor.VALUE, ms).target(4).ease(Linear.INOUT))
			.push(Tween.to(bfAlpha, BoxedFloatAccessor.VALUE, ms).target(0).ease(Linear.INOUT))
			.push(Tween.to(bfRot, BoxedFloatAccessor.VALUE, ms).target(-90).ease(Linear.INOUT))
			.setCallback(busyCallback)
			;
		//@on

		GameTweener.start(timeline);
	}
}
