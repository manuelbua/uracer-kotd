
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.utils.ShaderLoader;

public class TrackProgress extends Positionable implements Disposable {
	private HudLabel lblAdvantage;
	private float progressval, prevVal;// , speed, ghspeed;
	private float distPlayer, distGhost;
	private float prevDistPlayer, prevDistGhost;
	private float progressTargetVal, prevTargetVal;
	private boolean advantageShown;

	private final Texture texMask;
	private final ShaderProgram shProgress;
	private final Sprite sAdvantage, sProgress;
	private boolean flipped;

	private final float scale;
	private String customMessage = "";

	public TrackProgress (float scale) {

		this.scale = scale;
		lblAdvantage = new HudLabel(scale, FontFace.CurseWhiteBig, "", false, 2f);
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

	// public void setPlayerSpeed (float mts) {
	// speed = mts;
	// }
	//
	// public void setTargetSpeed (float mts) {
	// ghspeed = mts;
	// }

	public void setPlayerDistance (float mt) {
		distPlayer = AMath.fixup(AMath.lerp(prevDistPlayer, mt, 0.25f));
		prevDistPlayer = distPlayer;
	}

	public void setTargetDistance (float mt) {
		distGhost = AMath.fixup(AMath.lerp(prevDistGhost, mt, 0.25f));
		prevDistGhost = distGhost;
	}

	/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting line, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setPlayerProgression (float progress) {
		// smooth out high freq
		progressval = AMath.fixup(AMath.lerp(prevVal, progress, 0.25f));
		prevVal = progressval;
	}

	/** Sets the target's progression in the range [0,1] inclusive, to indicate car's track progress. 0 means on starting line, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setTargetProgression (float progress) {
		// smooth out high freq
		progressTargetVal = AMath.fixup(AMath.lerp(prevTargetVal, progress, 0.25f));
		prevTargetVal = progressTargetVal;
	}

	public void resetCounters (boolean resetState) {
		progressval = 0;
		progressTargetVal = 0;
		distGhost = 0;
		distPlayer = 0;

		if (resetState) {
			prevDistGhost = 0;
			prevDistPlayer = 0;
			prevVal = 0;
			prevTargetVal = 0;
		}
	}

	public void tick () {
		lblAdvantage.tick();
	}

	public void setMessage (String messageOrEmpty) {
		customMessage = messageOrEmpty;
	}

	public void render (SpriteBatch batch, float cameraZoom) {

		float playerToTarget = 0;

		// float a = 1f - 0.7f * URacer.Game.getTimeModFactor();
		float a = 0.25f;

		playerToTarget = AMath.fixup(progressval - progressTargetVal);
		if (customMessage.length() == 0) {
			lblAdvantage.setString(Math.round(distPlayer - distGhost) + " mt");
		} else {
			lblAdvantage.setString(customMessage);
		}

		if (distPlayer > 0) {
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
		lblAdvantage.setPosition(position.x, position.y - cameraZoom * Convert.scaledPixels(90) - Convert.scaledPixels(90)
			* timeFactor * cameraZoom - Convert.scaledPixels(8) * cameraZoom);
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
		shProgress.setUniformf("progress", progressval);
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
