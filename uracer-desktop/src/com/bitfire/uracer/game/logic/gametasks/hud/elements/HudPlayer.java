
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.BasicInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.VMath;

/** Encapsulates player's drifting information shown on screen */
public final class HudPlayer extends HudElement {
	public enum EndDriftType {
		GoodDrift, BadDrift
	}

	// we need an HudLabel circular buffer since
	// the player could be doing combos and the time
	// needed for one single labelResult to ".slide"
	// and disappear could be higher than the time
	// needed for the user to initiate, perform and
	// finish the next drift.. in this case the label
	// will move from the last result position to the
	// current one
	private static final int MaxLabelResult = 3;

	// player info
	private EntityRenderState playerState = null;

	// presentation
	private final BasicInfo basicInfo;
	public final DriftBar driftBar;

	private HudLabel labelRealtime, labelSpeed, labelDistance;
	private HudLabel[] labelResult;

	private int nextLabelResult = 0;

	private PlayerCar player;
	private UserProfile userProfile;

	//
	private final GameRenderer renderer;

	// gravitation
	private float carModelWidthPx, carModelLengthPx;
	private Vector2 tmpg = new Vector2();
	private Vector2 tmpg2 = new Vector2();

	private Vector2 lastRealtimePos = new Vector2();
	private boolean began = false;
	private float scale = 1f;

	public HudPlayer (UserProfile userProfile, ScalingStrategy scalingStrategy, PlayerCar player, GameRenderer renderer) {
		this.userProfile = userProfile;
		this.player = player;
		this.renderer = renderer;
		this.scale = scalingStrategy.invTileMapZoomFactor;
		playerState = player.state();

		this.carModelWidthPx = Convert.mt2px(player.getCarModel().width);
		this.carModelLengthPx = Convert.mt2px(player.getCarModel().length);

		basicInfo = new BasicInfo(scale, userProfile);
		driftBar = new DriftBar(scale, carModelLengthPx);

		// labelRealtime role is to display PlayerCar values in real-time!
		labelRealtime = new HudLabel(scale, FontFace.Arcade, "+99.99", false, 0.5f);
		labelRealtime.setAlpha(0);
		lastRealtimePos.set(0, 0);

		labelResult = new HudLabel[MaxLabelResult];
		nextLabelResult = 0;
		for (int i = 0; i < MaxLabelResult; i++) {
			labelResult[i] = new HudLabel(scale, FontFace.CurseRed, "+99.99", false, 0.85f);
			labelResult[i].setAlpha(0);
		}

		labelSpeed = new HudLabel(scale, FontFace.Lcd, "", true, 1f);
		labelSpeed.setPosition(Gdx.graphics.getWidth() - Convert.scaledPixels(190),
			Gdx.graphics.getHeight() - Convert.scaledPixels(110));

		labelDistance = new HudLabel(scale, FontFace.Lcd, "", true, 0.85f);
		labelDistance.setPosition(Gdx.graphics.getWidth() - Convert.scaledPixels(190),
			Gdx.graphics.getHeight() - Convert.scaledPixels(50));
	}

	@Override
	public void dispose () {
		driftBar.dispose();
		basicInfo.dispose();
	}

	@Override
	public void onTick () {
		updateLabelRealtime(false);
// updateLabelRealtime(true);
	}

	private void updateLabelRealtime (boolean force) {
		if (force || (began && labelRealtime.isVisible())) {
			labelRealtime.setString("+" + NumberString.format(player.driftState.driftSeconds()));
// labelRealtime.setString("99999.99999");
			if (force) {
				labelRealtime.setAlpha(1);
			}
		}
	}

	@Override
	public void onReset () {
		labelRealtime.setAlpha(0);
		for (int i = 0; i < MaxLabelResult; i++) {
			labelResult[i].setAlpha(0);
		}

		nextLabelResult = 0;
	}

	@Override
	public void onRender (SpriteBatch batch) {

		basicInfo.render(batch);
		bottom(driftBar, 50);
		driftBar.render(batch, renderer.getWorldRenderer().getCameraZoom());

		labelRealtime.setFont(FontFace.Lcd);
		labelRealtime.setScale(0.5f * renderer.getWorldRenderer().getCameraZoom(), true);
		bottom(labelRealtime, 80);
		// gravitate(labelRealtime, 90, 0);

		lastRealtimePos.set(labelRealtime.getPosition());

		// draw earned/lost seconds
		if (labelRealtime.isVisible()) {
			labelRealtime.render(batch);
		}

		// draw player name+info
		labelSpeed.setString(MathUtils.round(CarUtils.mtSecToKmHour(player.getInstantSpeed())) + " kmh");
		labelSpeed.render(batch);

		labelDistance.setString(MathUtils.round(player.getTraveledDistance()) + " mt\n");
		labelDistance.render(batch);

		// draw result
		for (int i = 0; i < MaxLabelResult; i++) {
			labelResult[i].render(batch);
		}
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
		labelRealtime.fadeIn(300);
		began = true;
	}

	/** Signals the hud element that the player has finished drifting */
	public void endDrift (String message, EndDriftType type) {
// HudLabel result = labelResult[nextLabelResult++];
//
// if (nextLabelResult == MaxLabelResult) {
// nextLabelResult = 0;
// }
//
// switch (type) {
// case BadDrift:
// result.setFont(BitmapFontFactory.get(FontFace.CurseRedBig));
// break;
// case GoodDrift:
// default:
// result.setFont(BitmapFontFactory.get(FontFace.CurseGreenBig));
// break;
// }
//
// result.setString(message);
// result.setPosition(lastRealtimePos);
// result.slide(type == EndDriftType.GoodDrift);
//
		began = false;
		updateLabelRealtime(true);

		labelRealtime.fadeOut(300);
	}

}
