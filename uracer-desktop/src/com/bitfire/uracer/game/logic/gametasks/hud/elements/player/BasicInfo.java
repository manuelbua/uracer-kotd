
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;

/** Displays basic information such as player name, nation flag */
public class BasicInfo implements Disposable {
	private HudLabel name;
	private TextureRegion flag;
	private float borderX, borderY;
	private float w, h;

	public BasicInfo (UserProfile profile) {
		name = new HudLabel(FontFace.CurseRedYellowBig, profile.userName, true);
		flag = Art.getFlag(profile.userCountryCode);
		borderX = 15;// Convert.scaledPixels(15);
		borderY = 5;// Convert.scaledPixels(5);
		w = 80;// Convert.scaledPixels(80);
		h = 80;// Convert.scaledPixels(80);
		// name.setPosition(w + borderX * 2 + name.getWidth() / 2, Convert.scaledPixels(42));
		name.setPosition(w + borderX * 2 + name.getWidth() / 2, 42);
	}

	@Override
	public void dispose () {
	}

	public void render (SpriteBatch batch) {
		batch.draw(flag, borderX, borderY, w, h);
		name.render(batch);
	}
}
