
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.gametasks.hud.Hud;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.BasicInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.TrackProgress;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.WrongWay;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.VMath;

/** Encapsulates player's drifting information shown on screen */
public final class HudPlayer extends HudElement {
	public enum EndDriftType {
		GoodDrift, BadDrift
	}

	// player info
	private EntityRenderState playerState = null;

	// player elements
	private final BasicInfo basicInfo;
	public final WrongWay wrongWay;
	public final DriftBar driftBar;
	public final TrackProgress trackProgress;

	private HudLabel labelSpeed, labelDistance;

	private PlayerCar player;
	private CarHighlighter highlightError;
	private CarHighlighter highlightNext;

	//
	private final GameRenderer renderer;

	// gravitation
	private float carModelWidthPx, carModelLengthPx;
	private Vector2 tmpg = new Vector2();

	private float scale = 1f;

	public HudPlayer (UserProfile userProfile, ScalingStrategy scalingStrategy, PlayerCar player, GameRenderer renderer) {
		this.player = player;
		this.renderer = renderer;
		this.scale = scalingStrategy.invTileMapZoomFactor;
		playerState = player.state();

		this.carModelWidthPx = Convert.mt2px(player.getCarModel().width);
		this.carModelLengthPx = Convert.mt2px(player.getCarModel().length);

		// elements
		basicInfo = new BasicInfo(scale, userProfile);
		wrongWay = new WrongWay();
		driftBar = new DriftBar(scale, carModelLengthPx);
		trackProgress = new TrackProgress(scale);

		// labels
		labelSpeed = new HudLabel(scale, FontFace.Roboto, "", true, 1f);
		labelSpeed.setPosition(Gdx.graphics.getWidth() - Convert.scaledPixels(190),
			Gdx.graphics.getHeight() - Convert.scaledPixels(110));

		labelDistance = new HudLabel(scale, FontFace.Roboto, "", true, 0.85f);
		labelDistance.setPosition(Gdx.graphics.getWidth() - Convert.scaledPixels(190),
			Gdx.graphics.getHeight() - Convert.scaledPixels(50));

		highlightError = new CarHighlighter();
		highlightError.setCar(player);
		highlightError.setScale(1.75f);

		highlightNext = new CarHighlighter();
		highlightNext.setScale(1);
	}

	@Override
	public void dispose () {
		trackProgress.dispose();
		driftBar.dispose();
		basicInfo.dispose();
	}

	@Override
	public void onTick () {
		driftBar.tick();
		trackProgress.tick();
	}

	@Override
	public void onReset () {
		driftBar.hideSecondsLabel();
		highlightError.stop();
		highlightNext.stop();
		wrongWay.fadeOut(Hud.DefaultFadeMilliseconds);
	}

	@Override
	public void onRender (SpriteBatch batch) {

		float cz = renderer.getWorldRenderer().getCameraZoom();

		basicInfo.render(batch);

		atPlayer(driftBar);
		driftBar.render(batch, cz);

		atPlayer(trackProgress);
		trackProgress.render(batch, cz);

		// draw player name+info
		labelSpeed.setString(MathUtils.round(CarUtils.mtSecToKmHour(player.getInstantSpeed())) + " kmh");
		labelSpeed.render(batch);

		labelDistance.setString(MathUtils.round(player.getTraveledDistance()) + " mt\n");
		labelDistance.render(batch);

		highlightError.render(batch, cz);
		highlightNext.render(batch, cz);

		wrongWay.render(batch);
	}

	//
	// internal helpers
	//

	private void bottom (Positionable p, float distance) {
		float zs = renderer.getWorldRenderer().getCameraZoom();

		tmpg.set(GameRenderer.ScreenUtils.worldPxToScreen(playerState.position));
		tmpg.y += distance * scale * zs;
		p.setPosition(tmpg);
	}

	private void gravitate (Positionable p, float offsetDegs, float distance) {
		p.setPosition(gravitate(p.getWidth(), p.getHeight(), offsetDegs, distance));
	}

	private void atPlayer (Positionable p) {
		tmpg.set(GameRenderer.ScreenUtils.worldPxToScreen(playerState.position));
		p.setPosition(tmpg);
	}

	/** Returns a position by placing a point on an imaginary circumference gravitating around the player, applying the specified
	 * orientation offset, expressed in radians, if any. */
	private Vector2 gravitate (float w, float h, float offsetDegs, float distance) {
		float zs = renderer.getWorldRenderer().getCameraZoom();
		float border = distance * scale;

		Vector2 sp = GameRenderer.ScreenUtils.worldPxToScreen(playerState.position);
		Vector2 heading = VMath.fromDegrees(playerState.orientation + offsetDegs);

		float horizontal = MathUtils.clamp(Math.abs(MathUtils.sinDeg(playerState.orientation)), 0.25f, 1);
		float p = AMath.lerp(carModelWidthPx, carModelLengthPx, horizontal);
		float q = AMath.lerp(carModelWidthPx, carModelLengthPx, 1 - horizontal);

		// compute displacement
		tmpg.set(heading);
		float displaceX = p * zs + w * 0.5f + border;
		float displaceY = q * zs + h * 0.5f + border;
		tmpg.mul(displaceX, displaceY);
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
	public void endDrift (String message, EndDriftType type) {
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
		highlightNext.track();
	}

	public void unHighlightNextTarget (Car car) {
		highlightNext.untrack();
	}

	public void setNextTargetAlpha (float alpha) {
		highlightNext.setAlpha(alpha);
	}
}
