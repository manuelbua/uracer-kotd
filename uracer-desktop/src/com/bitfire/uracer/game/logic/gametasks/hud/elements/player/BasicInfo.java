
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.Convert;

/** Displays basic information such as player name, nation flag */
public class BasicInfo implements Disposable {
	private final float invTilemapZoom;

	private HudLabel name;
	private TextureRegion flag;
	private float borderX, borderY;
	private float w, h;

	public BasicInfo (float invTilemapZoomFactor, UserProfile profile) {
		this.invTilemapZoom = invTilemapZoomFactor;
		name = new HudLabel(invTilemapZoom, FontFace.CurseRedYellowBig, profile.userName, true);
		flag = Art.getFlag(profile.userCountryCode);
		borderX = Convert.scaledPixels(15);
		borderY = Convert.scaledPixels(5);
		w = Convert.scaledPixels(80);
		h = Convert.scaledPixels(80);
		name.setPosition(w + borderX * 2 + name.getHalfWidth(), Convert.scaledPixels(42));
	}

	@Override
	public void dispose () {
	}

	public void render (SpriteBatch batch) {
		batch.draw(flag, borderX, borderY, w, h);
		name.render(batch);
	}
}
