
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

public class HudLapInfo extends HudElement {

	private float scale = 1f;
	private HudLabel curr;
	private LapInfo lapInfo;

	public HudLapInfo (ScalingStrategy scalingStrategy, LapInfo lapInfo) {
		this.lapInfo = lapInfo;
		scale = scalingStrategy.invTileMapZoomFactor;

		curr = new HudLabel(scalingStrategy.invTileMapZoomFactor, FontFace.LcdWhite, "99.99", true, 1.5f);
		curr.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - curr.getHalfHeight() - Convert.scaledPixels(10)
			* scale);
	}

	@Override
	public void dispose () {
	}

	@Override
	public void onRender (SpriteBatch batch) {
		// current time
		curr.setString(NumberString.format(lapInfo.getElapsedSeconds()), true);
		curr.render(batch);
	}
}
