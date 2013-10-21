
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.BasicInfo;

public class HudPlayerStatic extends HudElement {
	private BasicInfo basicInfo;

	// private HudLabel labelSpeed, labelDistance;

	public HudPlayerStatic (UserProfile userProfile) {
		basicInfo = new BasicInfo(userProfile);
		// labelSpeed = new HudLabel(FontFace.Roboto, "", true);
		// labelSpeed.setPosition(Config.Graphics.ReferenceScreenWidth - 190, Config.Graphics.ReferenceScreenHeight - 110);
		// labelDistance = new HudLabel(FontFace.Roboto, "", true);
		// labelDistance.setScale(0.85f);
		// labelDistance.setPosition(Config.Graphics.ReferenceScreenWidth - 190, Config.Graphics.ReferenceScreenHeight - 50);
	}

	@Override
	public void dispose () {
		basicInfo.dispose();
	}

	@Override
	public void onRender (SpriteBatch batch, float cameraZoom) {
		if (hasPlayer) {
			basicInfo.render(batch);

			// labelSpeed.setString(MathUtils.round(CarUtils.mtSecToKmHour(player.getInstantSpeed())) + " kmh");
			// labelSpeed.render(batch);
			//
			// labelDistance.setString(MathUtils.round(player.getTraveledDistance()) + " mt\n");
			// labelDistance.render(batch);
		}
	}
}
