
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
import com.bitfire.utils.ShaderLoader;

public class TrackProgress extends Positionable {
	private HudLabel lblAdvantage;
	private boolean advantageShown;

	private final Texture texMask;
	private final ShaderProgram shProgress;
	private final Sprite sAdvantage, sProgress;
	private boolean flipped;

	private String customMessage = "";
	private TrackProgressData data = null;

	/** Data needed by this component */
	public static class TrackProgressData {
		public float playerDistance, targetDistance;
		public float playerProgress, targetProgress;

		public void reset (boolean resetState) {
			playerDistance = 0;
			targetDistance = 0;
			playerProgress = 0;
			targetProgress = 0;

			if (resetState) {
				prevpd = 0;
				prevpp = 0;
				prevtd = 0;
				prevtp = 0;
			}
		}

		public void setPlayerDistance (float mt) {
			playerDistance = AMath.fixup(AMath.lerp(prevpd, mt, Smoothing));
			prevpd = playerDistance;
		}

		public void setTargetDistance (float mt) {
			targetDistance = AMath.fixup(AMath.lerp(prevtd, mt, Smoothing));
			prevtd = targetDistance;
		}

		/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting
		 * line, 1 means finished.
		 * @param progress The progress so far */
		public void setPlayerProgression (float progress) {
			// smooth out high freq
			playerProgress = AMath.fixup(AMath.lerp(prevpp, progress, Smoothing));
			prevpp = playerProgress;
		}

		/** Sets the target's progression in the range [0,1] inclusive, to indicate target's track progress. 0 means on starting
		 * line, 1 means finished.
		 * @param progress The progress so far */
		public void setTargetProgression (float progress) {
			// smooth out high freq
			targetProgress = AMath.fixup(AMath.lerp(prevtp, progress, Smoothing));
			prevtp = targetProgress;
		}

		private static final float Smoothing = 0.25f;
		private float prevpd, prevtd;
		private float prevpp, prevtp;
	}

	public TrackProgress (float scale) {

		setScale(scale);
		lblAdvantage = new HudLabel(FontFace.CurseWhiteBig, "", false);
		advantageShown = false;
		lblAdvantage.setAlpha(1);

		texMask = Art.texCircleProgressMask;

		shProgress = ShaderLoader.fromFile("progress", "progress");

		sAdvantage = new Sprite(Art.texCircleProgress);
		sAdvantage.flip(false, true);
		flipped = false;

		sProgress = new Sprite(Art.texRadLinesProgress);
		sProgress.flip(false, true);
	}

	@Override
	public void dispose () {
		shProgress.dispose();
	}

	public void tick (TrackProgressData data) {
		lblAdvantage.tick();
		this.data = data;
	}

	public void setMessage (String messageOrEmpty) {
		customMessage = messageOrEmpty;
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

		float playerToTarget = 0;

		// float a = 1f - 0.7f * URacer.Game.getTimeModFactor();
		float a = 0.25f;

		playerToTarget = AMath.fixup(data.playerProgress - data.targetProgress);
		if (customMessage.length() == 0) {
			lblAdvantage.setString(Math.round(data.playerDistance - data.targetDistance) + " mt");
		} else {
			lblAdvantage.setString(customMessage);
		}

		if (data.playerDistance > 0) {
			if (!advantageShown) {
				advantageShown = true;
				lblAdvantage.queueShow(500);
				// Gdx.app.log("", "show");
			}

		} else if (advantageShown) {
			advantageShown = false;
			lblAdvantage.queueHide(1000);
			// Gdx.app.log("", "hide");
		}

		// advantage/disadvantage
		float dist = MathUtils.clamp(playerToTarget * 8, -1, 1);
		float ndist = (dist + 1) * 0.5f;
		Color advantageColor = ColorUtils.paletteRYG(ndist * 2, 1f);

		float timeFactor = URacer.Game.getTimeModFactor() * 0.3f;

		lblAdvantage.setColor(advantageColor);
		lblAdvantage.setAlpha(a * 2);
		lblAdvantage.setScale(cameraZoom * (1f - 0.4f * (1 - ndist)));
		// lblAdvantage.setPosition(position.x, position.y - cameraZoom * Convert.scaledPixels(90) - Convert.scaledPixels(90) *
		// timeFactor * cameraZoom - Convert.scaledPixels(8) * cameraZoom);
		lblAdvantage.setPosition(position.x, position.y - cameraZoom * 90 - 90 * timeFactor * cameraZoom - 8 * cameraZoom);
		lblAdvantage.render(batch);

		float s = 1f + timeFactor;
		float scl = cameraZoom * scale * s;

		// dbg
		// dist = 0.35f;
		// progressval = 0.5f;
		// distGhost = 0.15f;
		// distanceFromBest = 0.15f;

		batch.setShader(shProgress);

		// set mask
		texMask.bind(1);
		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0);
		shProgress.setUniformi("u_texture1", 1);

		scl += .07f * URacer.Game.getTimeModFactor();

		// player's progress
		shProgress.setUniformf("progress", data.playerProgress);
		sProgress.setColor(Color.WHITE);
		sProgress.setScale(scl);
		sProgress.setPosition(position.x - sProgress.getWidth() / 2, position.y - sProgress.getHeight() / 2);
		sProgress.draw(batch, a);
		batch.flush();

		boolean isBack = (dist < 0);
		if (isBack && !flipped) {
			flipped = true;
			sAdvantage.flip(true, false);
		} else if (!isBack && flipped) {
			flipped = false;
			sAdvantage.flip(true, false);
		}

		shProgress.setUniformf("progress", Math.abs(playerToTarget));
		sAdvantage.setColor(advantageColor);
		sAdvantage.setScale(scl * 1.1f);
		sAdvantage.setPosition(position.x - sAdvantage.getWidth() / 2, position.y - sAdvantage.getHeight() / 2);
		sAdvantage.draw(batch, a);
		batch.flush();

		batch.setShader(null);
	}
}
