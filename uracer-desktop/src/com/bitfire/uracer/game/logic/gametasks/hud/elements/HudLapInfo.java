
package com.bitfire.uracer.game.logic.gametasks.hud.elements;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

public class HudLapInfo extends HudElement {

	private float scale = 1f;
	private HudLabel curr, progress;
	private LapInfo lapInfo;
	private boolean isProgressGood; // cache, avoid resetting the font

	public HudLapInfo (ScalingStrategy scalingStrategy, LapInfo lapInfo) {
		this.lapInfo = lapInfo;
		scale = scalingStrategy.invTileMapZoomFactor;

		curr = new HudLabel(scalingStrategy.invTileMapZoomFactor, FontFace.LcdWhite, "99.99", true, 1.5f);
		curr.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() - curr.getHalfHeight() - Convert.scaledPixels(10)
			* scale);

		progress = new HudLabel(scalingStrategy.invTileMapZoomFactor, FontFace.CurseGreen, "100%", true, 2f);
// progress.setPosition(progress.getHalfWidth() + Convert.scaledPixels(10) * scale,
// Gdx.graphics.getHeight() - progress.getHalfHeight() - Convert.scaledPixels(20) * scale);
		progress.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
	}

	@Override
	public void dispose () {
	}

	/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting ine, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setPlayerProgression (float progress) {
		this.progress.setString(MathUtils.round(progress * 100) + "%", true);
	}

	public void setProgressionGood () {
		if (!isProgressGood) {
			isProgressGood = true;
			progress.setFont(FontFace.CurseGreenBig);
		}
	}

	public void setProgressionBad () {
		if (isProgressGood) {
			isProgressGood = false;
			progress.setFont(FontFace.CurseRedBig);
		}
	}

	@Override
	public void onTick () {
		// current time
		curr.setString(NumberString.format(lapInfo.getElapsedSeconds()), true);
	}

	@Override
	public void onRender (SpriteBatch batch) {
		curr.render(batch);

		progress.setAlpha(0.5f);
		progress.setScale(5);
		progress.render(batch);
	}

	@Override
	public void onReset () {
	}
}
