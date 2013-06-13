
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.utils.ShaderLoader;

public class TrackProgress extends Positionable {
	private HudLabel lblAdvantage;
	private boolean lblAdvantageShown;

	private final Texture texMask;
	private final ShaderProgram shProgress;
	private final Sprite sprAdvantage, sprProgress;
	private boolean flipped, hasTarget;

	private String customMessage = "";
	private TrackProgressData data = new TrackProgressData();

	/** Data needed by this component */
	public static class TrackProgressData {

		private static final float Smoothing = 0.25f;
		private InterpolatedFloat playerDistance, targetDistance;
		private InterpolatedFloat playerProgress, targetProgress;

		public TrackProgressData () {
			playerDistance = new InterpolatedFloat();
			targetDistance = new InterpolatedFloat();
			playerProgress = new InterpolatedFloat();
			targetProgress = new InterpolatedFloat();
		}

		public void reset (boolean resetState) {
			playerDistance.reset(0, resetState);
			targetDistance.reset(0, resetState);
			playerProgress.reset(0, resetState);
			targetProgress.reset(0, resetState);
		}

		public void setPlayerDistance (float mt) {
			playerDistance.set(mt, Smoothing);
		}

		public void setTargetDistance (float mt) {
			targetDistance.set(mt, Smoothing);
		}

		/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting
		 * line, 1 means finished.
		 * @param progress The progress so far */
		public void setPlayerProgression (float progress) {
			playerProgress.set(progress, Smoothing);
		}

		/** Sets the target's progression in the range [0,1] inclusive, to indicate target's track progress. 0 means on starting
		 * line, 1 means finished.
		 * @param progress The progress so far */
		public void setTargetProgression (float progress) {
			targetProgress.set(progress, Smoothing);
		}
	}

	public TrackProgress () {
		lblAdvantage = new HudLabel(FontFace.CurseWhiteBig, "", false);
		lblAdvantageShown = false;
		hasTarget = false;
		lblAdvantage.setAlpha(0);

		texMask = Art.texCircleProgressMask;

		shProgress = ShaderLoader.fromFile("progress", "progress");

		sprAdvantage = new Sprite(Art.texCircleProgress);
		sprAdvantage.flip(false, true);
		flipped = false;

		sprProgress = new Sprite(Art.texRadLinesProgress);
		sprProgress.flip(false, true);
	}

	@Override
	public void dispose () {
		shProgress.dispose();
	}

	public TrackProgressData getProgressData () {
		return data;
	}

	public void setCustomMessage (String messageOrEmpty) {
		customMessage = messageOrEmpty;
	}

	public void hideCustomMessage () {
		customMessage = "";
	}

	public void setHasTarget (boolean hasTarget) {
		this.hasTarget = hasTarget;
	}

	@Override
	public float getWidth () {
		return 0;
	}

	@Override
	public float getHeight () {
		return 0;
	}

	public void render (SpriteBatch batch, float cameraZoom) {
		if (data == null) {
			return;
		}

		// advantage/disadvantage
		float timeFactor = URacer.Game.getTimeModFactor() * 0.3f;

		// advantage if > 0, disadvantage if < 0
		float playerToTarget = AMath.fixup(data.playerProgress.get() - data.targetProgress.get());
		float dist = MathUtils.clamp(playerToTarget, -1, 1);
		Color advantageColor = ColorUtils.paletteRYG(dist + 1, 1f);

		float adist = Math.abs(dist);
		float s = cameraZoom;
		if (dist < 0) {
			s += 0.5f * adist;
		}

		boolean showAdv = true;
		if (customMessage.length() == 0) {
			if (hasTarget) {
				float v = data.playerDistance.get() - data.targetDistance.get();
				lblAdvantage.setString(NumberString.format(v) + " m", false);
			} else {
				showAdv = false;
			}
		} else {
			lblAdvantage.setString(customMessage);
		}

		if (showAdv) {
			if (!lblAdvantageShown) {
				lblAdvantageShown = true;
				lblAdvantage.fadeIn(500);
			}
		} else if (lblAdvantageShown) {
			lblAdvantageShown = false;
			lblAdvantage.fadeOut(1000);
		}

		if (lblAdvantage.getAlpha() > 0) {
			lblAdvantage.setColor(advantageColor);
			lblAdvantage.setScale(s);
			lblAdvantage.setPosition(position.x, position.y - cameraZoom * 100 - cameraZoom * 100 * timeFactor - cameraZoom * 20
				* adist);
			lblAdvantage.render(batch);
		}

		float scl = cameraZoom * scale * (1f + timeFactor);

		batch.setShader(shProgress);

		// set mask
		texMask.bind(1);
		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0);
		shProgress.setUniformi("u_texture1", 1);

		scl += .07f * URacer.Game.getTimeModFactor();

		// player's track progress
		shProgress.setUniformf("progress", data.playerProgress.get());
		sprProgress.setColor(Color.WHITE);
		sprProgress.setScale(scl);
		sprProgress.setPosition(position.x - sprProgress.getWidth() / 2, position.y - sprProgress.getHeight() / 2);
		sprProgress.draw(batch, 0.5f);
		batch.flush();

		boolean isBack = (dist < 0);
		if (isBack && !flipped) {
			flipped = true;
			sprAdvantage.flip(true, false);
		} else if (!isBack && flipped) {
			flipped = false;
			sprAdvantage.flip(true, false);
		}

		// player's advantage/disadvantage
		if (hasTarget) {
			shProgress.setUniformf("progress", Math.abs(playerToTarget));
			sprAdvantage.setColor(advantageColor);
			sprAdvantage.setScale(scl * 1.1f);
			sprAdvantage.setPosition(position.x - sprAdvantage.getWidth() / 2, position.y - sprAdvantage.getHeight() / 2);
			sprAdvantage.draw(batch, 1);
			batch.flush();
		}

		batch.setShader(null);
	}
}
