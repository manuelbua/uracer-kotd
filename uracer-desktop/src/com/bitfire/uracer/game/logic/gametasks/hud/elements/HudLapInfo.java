
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.resources.BitmapFontFactory;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.NumberString;

public class HudLapInfo extends HudElement {

	private HudLabel best, curr, last;
	private LapInfo lapInfo;

	public HudLapInfo (ScalingStrategy scalingStrategy, LapInfo lapInfo) {
		this.lapInfo = lapInfo;

		int gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		// laptimes component
		BitmapFont f = BitmapFontFactory.get(FontFace.CurseRedYellowNew);
		best = new HudLabel(scalingStrategy, f, "BEST  TIME\n--.--", true);
		curr = new HudLabel(scalingStrategy, f, "YOUR  TIME\n--.--", true);
		last = new HudLabel(scalingStrategy, f, "LAST  TIME\n--.--", true);

		curr.setPosition(gridX, 50 * scalingStrategy.invTileMapZoomFactor);
		last.setPosition(gridX * 3, 50 * scalingStrategy.invTileMapZoomFactor);
		best.setPosition(gridX * 4, 50 * scalingStrategy.invTileMapZoomFactor);
	}

	@Override
	public void dispose () {
	}

	@Override
	public void onTick () {
		// current time
		curr.setString("YOUR  TIME\n" + NumberString.format(lapInfo.getElapsedSeconds()) + "s");

		// best time
		if (lapInfo.hasBestTrackTimeSeconds()) {
			// has best
			best.setString("BEST  TIME\n" + NumberString.format(lapInfo.getBestTrackTimeSeconds()) + "s");
		} else {
			// temporarily use last track time
			if (lapInfo.hasLastTrackTimeSeconds()) {
				best.setString("BEST  TIME\n" + NumberString.format(lapInfo.getLastTrackTimeSeconds()) + "s");
			} else {
				best.setString("BEST TIME\n--:--");
			}
		}

		// last time
		if (lapInfo.hasLastTrackTimeSeconds()) {
			// has only last
			last.setString("LAST  TIME\n" + NumberString.format(lapInfo.getLastTrackTimeSeconds()) + "s");
		} else {
			last.setString("LAST  TIME\n--:--");
		}
	}

	@Override
	public void onRender (SpriteBatch batch) {
		curr.render(batch);
		best.render(batch);
		last.render(batch);
	}

	@Override
	public void onReset () {
	}
}
