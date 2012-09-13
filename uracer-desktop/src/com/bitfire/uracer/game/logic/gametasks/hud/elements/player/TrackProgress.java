
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;

public class TrackProgress extends Positionable implements Disposable {
	private HudLabel progress;
	private float progressval;
	private boolean isProgressGood; // cache, avoid resetting the font

	public TrackProgress (float scale) {

		progress = new HudLabel(scale, FontFace.CurseGreen, "100%", true, 2f);
		// progress.setPosition(progress.getHalfWidth() + Convert.scaledPixels(10) * scale,
		// Gdx.graphics.getHeight() - progress.getHalfHeight() - Convert.scaledPixels(20) * scale);
// progress.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
	}

	@Override
	public void dispose () {
	}

	/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting ine, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setPlayerProgression (float progress) {
		progressval = progress;
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

	public void render (SpriteBatch batch, float cameraZoom) {
		progress.setString(MathUtils.round(progressval * 100) + "%", true);
		progress.setScale(cameraZoom * 0.6f);
		progress.setPosition(position.x, position.y);
		progress.render(batch);
	}
}
