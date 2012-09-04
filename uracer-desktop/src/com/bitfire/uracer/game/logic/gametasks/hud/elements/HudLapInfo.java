
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.NumberString;

public class HudLapInfo extends HudElement {

	private float scale = 1f;
	private HudLabel curr;// , best, last;
	private LapInfo lapInfo;

	public HudLapInfo (ScalingStrategy scalingStrategy, LapInfo lapInfo) {
		this.lapInfo = lapInfo;
		scale = scalingStrategy.invTileMapZoomFactor;

		int gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		// laptimes component
// best = new HudLabel(scalingStrategy, FontFace.CurseRedYellowNew, "BEST  TIME\n--.--", true);
// last = new HudLabel(scalingStrategy, FontFace.CurseRedYellowNew, "LAST  TIME\n--.--", true);

		curr = new HudLabel(scalingStrategy.invTileMapZoomFactor, FontFace.LcdWhite, "99.99", true, 1.5f);
		curr.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - curr.getHalfHeight() - 10 * scale);

// last.setPosition(gridX * 3, 50 * scalingStrategy.invTileMapZoomFactor);
// best.setPosition(gridX * 4, 50 * scalingStrategy.invTileMapZoomFactor);
	}

	@Override
	public void dispose () {
	}

	@Override
	public void onTick () {
		// current time
		curr.setString(NumberString.format(lapInfo.getElapsedSeconds()), true);

// // best time
// if (lapInfo.hasBestTrackTimeSeconds()) {
// // has best
// best.setString("BEST  TIME\n" + NumberString.format(lapInfo.getBestTrackTimeSeconds()) + "s");
// } else {
// // temporarily use last track time
// if (lapInfo.hasLastTrackTimeSeconds()) {
// best.setString("BEST  TIME\n" + NumberString.format(lapInfo.getLastTrackTimeSeconds()) + "s");
// } else {
// best.setString("BEST TIME\n--:--");
// }
// }
//
// // last time
// if (lapInfo.hasLastTrackTimeSeconds()) {
// // has only last
// last.setString("LAST  TIME\n" + NumberString.format(lapInfo.getLastTrackTimeSeconds()) + "s");
// } else {
// last.setString("LAST  TIME\n--:--");
// }
	}

	@Override
	public void onRender (SpriteBatch batch) {
		curr.render(batch);
// best.render(batch);
// last.render(batch);
	}

	@Override
	public void onReset () {
	}
}
