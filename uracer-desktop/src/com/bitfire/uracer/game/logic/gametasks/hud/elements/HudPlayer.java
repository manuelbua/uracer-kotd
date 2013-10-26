
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.TrackProgress;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.WrongWay;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.VMath;

/** Encapsulates player's information shown on screen moving along the player position */
public final class HudPlayer extends HudElement {
	// player info
	private EntityRenderState playerState = null;

	// player elements
	public final WrongWay wrongWay;
	public final DriftBar driftBar;
	public final TrackProgress trackProgress;

	private CarHighlighter highlightError;
	public CarHighlighter highlightNext;

	// gravitation
	private float carModelWidthPx, carModelLengthPx;
	private Vector2 tmpg = new Vector2();

	public HudPlayer (UserProfile userProfile) {
		// elements
		wrongWay = new WrongWay();
		driftBar = new DriftBar();
		trackProgress = new TrackProgress();

		highlightError = new CarHighlighter();
		highlightError.setScale(1.75f);

		highlightNext = new CarHighlighter();
		highlightNext.setScale(1);
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);
		if (hasPlayer) {
			playerState = player.state();
			carModelWidthPx = Convert.mt2px(player.getCarModel().width);
			carModelLengthPx = Convert.mt2px(player.getCarModel().length);
			highlightError.setCar(player);
		} else {
			driftBar.reset();
			onReset();
		}
	}

	@Override
	public void dispose () {
		trackProgress.dispose();
		driftBar.dispose();
	}

	@Override
	public void onRestart () {
		onReset();
	}

	@Override
	public void onReset () {
		driftBar.hideSecondsLabel();
		driftBar.reset();
		trackProgress.resetPlayerToTarget();
		highlightError.stop();
		highlightNext.stop();
		wrongWay.fadeOut(Config.Graphics.DefaultResetFadeMilliseconds);
	}

	@Override
	public void onRender (SpriteBatch batch, float cameraZoom) {
		if (hasPlayer) {
			// position elements at render time, so that source positions have been interpolated
			atPlayer(driftBar);
			atPlayer(trackProgress);
			gravitate(wrongWay, -180, 100, cameraZoom);

			trackProgress.render(batch, cameraZoom);
			driftBar.render(batch, cameraZoom);
		}

		highlightError.render(batch, cameraZoom);
		highlightNext.render(batch, cameraZoom);
		wrongWay.render(batch, cameraZoom);
	}

	//
	// internal position helpers
	//

	// private void bottom (Positionable p, float distance) {
	// float zs = renderer.getWorldRenderer().getCameraZoom();
	//
	// tmpg.set(GameRenderer.ScreenUtils.worldPxToScreen(playerState.position));
	// tmpg.y += distance * zs;
	// p.setPosition(tmpg);
	// }

	private void gravitate (Positionable p, float offsetDegs, float distance, float cameraZoom) {
		p.setPosition(gravitate(p.getWidth(), p.getHeight(), offsetDegs, distance, cameraZoom));
	}

	private void atPlayer (Positionable p) {
		tmpg.set(GameRenderer.ScreenUtils.worldPxToScreen(playerState.position));
		p.setPosition(tmpg);
	}

	/** Returns a position by placing a point on an imaginary circumference gravitating around the player, applying the specified
	 * orientation offset, expressed in degrees, if any. */
	private Vector2 gravitate (float w, float h, float offsetDegs, float distance, float cameraZoom) {
		float border = distance;

		Vector2 sp = GameRenderer.ScreenUtils.worldPxToScreen(playerState.position);
		Vector2 heading = VMath.fromDegrees(playerState.orientation + offsetDegs);

		float horizontal = MathUtils.clamp(Math.abs(MathUtils.sinDeg(playerState.orientation)), 0.25f, 1);
		float p = AMath.lerp(carModelWidthPx, carModelLengthPx, horizontal);
		float q = AMath.lerp(carModelWidthPx, carModelLengthPx, 1 - horizontal);

		// compute displacement
		tmpg.set(heading);
		float displaceX = p * cameraZoom + w * 0.5f + border;
		float displaceY = q * cameraZoom + h * 0.5f + border;
		tmpg.scl(displaceX, displaceY);
		displaceX = tmpg.x;
		displaceY = tmpg.y;

		tmpg.x = sp.x - displaceX;
		tmpg.y = sp.y - displaceY;

		return tmpg;
	}

	//
	// supported external operations
	//

	/** Signals the hud element that the player is initiating a drift */
	public void beginDrift () {
		driftBar.showSecondsLabel();
	}

	/** Signals the hud element that the player has finished drifting */
	public void endDrift () {
		driftBar.hideSecondsLabel();
	}

	public void highlightOutOfTrack () {
		highlightError.error(3);
	}

	public void highlightCollision () {
		highlightError.error(5);
	}

	public void highlightWrongWay () {
		highlightError.error(5);
	}

	public void highlightNextTarget (Car car) {
		highlightNext.setCar(car);

		// overwrite any possibly running untracking
		highlightNext.track(true, 0.5f);
	}

	public void unHighlightNextTarget () {
		// overwrite any possibly running tracking
		// i.e., ghost at finish line, player just behind it
		highlightNext.untrack(true);
	}
}
