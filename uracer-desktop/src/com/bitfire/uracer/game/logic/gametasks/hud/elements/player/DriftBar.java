
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.WindowedMean;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.utils.ShaderLoader;

public class DriftBar extends Positionable {
	public static final float MaxSeconds = 10f;
	public static final int MaxTicks = (int)(MaxSeconds * Config.Physics.TimestepHz);

	private float seconds;
	private HudLabel labelSeconds;
	private final WindowedMean driftStrength;

	private final Texture texHalf, texHalfMask;// , texProgress, texProgressMask;
	private final ShaderProgram shProgress;
	private final Sprite sProgress, sDriftStrength;
	private final float offX, offY, w, h;

	public DriftBar (float width) {
		seconds = 0;

		labelSeconds = new HudLabel(FontFace.CurseRedYellowNew, "s", false);
		labelSeconds.setAlpha(0);

		//
		texHalf = Art.texCircleProgressHalf;
		texHalfMask = Art.texCircleProgressHalfMask;
		// texProgress = Art.texCircleProgress;
		// texProgressMask = Art.texCircleProgressMask;

		w = texHalf.getWidth();
		h = texHalf.getHeight();
		offX = w / 2;
		offY = h / 2;
		shProgress = ShaderLoader.fromFile("progress", "progress");

		// drift points
		sProgress = new Sprite(texHalf);
		sProgress.flip(false, true);

		// drift strength
		driftStrength = new WindowedMean((int)(1 / 0.25f));
		sDriftStrength = new Sprite(texHalf);
	}

	@Override
	public float getWidth () {
		return 0;
	}

	@Override
	public float getHeight () {
		return 0;
	}

	public void setSeconds (float seconds) {
		this.seconds = MathUtils.clamp(seconds, 0, MaxSeconds);
	}

	public float getSeconds () {
		return seconds;
	}

	public void setDriftStrength (float strength) {
		driftStrength.addValue(strength);
	}

	public float getDriftStrength () {
		return driftStrength.getMean();
	}

	public void showSecondsLabel () {
		labelSeconds.fadeIn(300);
	}

	public void hideSecondsLabel () {
		labelSeconds.fadeOut(300);
	}

	@Override
	public void dispose () {
	}

	public void render (SpriteBatch batch, float cameraZoom) {
		float timeFactor = URacer.Game.getTimeModFactor() * 0.3f;
		float s = 0.55f + timeFactor * 0.5f;
		float scl = cameraZoom * s;

		labelSeconds.setScale(scl);
		labelSeconds.setString(NumberString.format(seconds) + "s", true);
		labelSeconds.setPosition(position.x, position.y + (90) * cameraZoom + (105) * timeFactor * cameraZoom);
		labelSeconds.render(batch);

		// circle progress for slow-mo accumulated time
		s = 0.8f + timeFactor;
		scl = cameraZoom * s;
		float px = position.x - offX;
		float py = position.y - offY + 32 * cameraZoom * s;

		batch.setShader(shProgress);
		texHalfMask.bind(1);
		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0);
		shProgress.setUniformi("u_texture1", 1);

		float ratio = seconds / MaxSeconds;
		shProgress.setUniformf("progress", ratio);
		sProgress.setColor(ColorUtils.paletteRYG(ratio, 1));
		sProgress.setScale(scl);
		sProgress.setPosition(px, py);
		sProgress.draw(batch, MathUtils.clamp(0.25f + 0.15f * ratio + 0.8f * URacer.Game.getTimeModFactor(), 0, 1));
		batch.flush();

		// drift strength
		float amount = driftStrength.getMean();
		if (!AMath.isZero(amount)) {
			py = position.y - offY - 32 * cameraZoom * s;
			shProgress.setUniformf("progress", MathUtils.clamp(amount, 0, 1));

			// float a = 1f - 0.7f * URacer.Game.getTimeModFactor(); // 0.5f + 0.5f * ratio
			float a = MathUtils.clamp(0.15f + amount * 0.7f, 0, 1);

			sDriftStrength.setColor(1, 1, 1, 1);
			sDriftStrength.setScale(scl);
			sDriftStrength.setPosition(px, py);
			sDriftStrength.draw(batch, a);
			batch.flush();
		}

		batch.setShader(null);
	}
}
