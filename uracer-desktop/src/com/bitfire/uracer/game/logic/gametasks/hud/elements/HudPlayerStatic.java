
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.BasicInfo;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.CarUtils;

public class HudPlayerStatic extends HudElement {
	private final BasicInfo basicInfo;
	private HudLabel labelSpeed, labelDistance;
	private PlayerCar player;
	// private final GameRenderer renderer;
	private float scale = 1f;

	public HudPlayerStatic (UserProfile userProfile, ScalingStrategy scalingStrategy, PlayerCar player, GameRenderer renderer) {
		this.player = player;
		// this.renderer = renderer;
		this.scale = scalingStrategy.invTileMapZoomFactor;

		basicInfo = new BasicInfo(userProfile);

		labelSpeed = new HudLabel(FontFace.Roboto, "", true);
		// labelSpeed.setPosition(Gdx.graphics.getWidth() - Convert.scaledPixels(190),
		// Gdx.graphics.getHeight() - Convert.scaledPixels(110));
		// labelSpeed.setPosition(Gdx.graphics.getWidth() / scale - 190, Gdx.graphics.getHeight() / scale - 110);
		labelSpeed.setPosition(scalingStrategy.referenceResolution.x - 190, scalingStrategy.referenceResolution.y - 110);

		labelDistance = new HudLabel(FontFace.Roboto, "", true);
		labelDistance.setScale(0.85f);
		// labelDistance.setPosition(Gdx.graphics.getWidth() - Convert.scaledPixels(190),
		// Gdx.graphics.getHeight() - Convert.scaledPixels(50));
		labelDistance.setPosition(scalingStrategy.referenceResolution.x - 190, scalingStrategy.referenceResolution.y - 50);

	}

	@Override
	public void dispose () {
		basicInfo.dispose();
	}

	@Override
	public void onRender (SpriteBatch batch) {
		basicInfo.render(batch);

		labelSpeed.setString(MathUtils.round(CarUtils.mtSecToKmHour(player.getInstantSpeed())) + " kmh");
		labelSpeed.render(batch);

		labelDistance.setString(MathUtils.round(player.getTraveledDistance()) + " mt\n");
		labelDistance.render(batch);
	}
}
