
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
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.utils.ShaderLoader;

public class TrackProgress extends Positionable implements Disposable {
	private HudLabel lblAdvantage;
	private float progressval, prevVal, speed, ghspeed;
	private float distPlayer, distGhost;
	private float prevDistPlayer, prevDistGhost;
	private float progressTargetVal, prevTargetVal;
	private boolean advantageShown;

	private final Texture texMask;
	private final ShaderProgram shProgress;
	private final Sprite sAdvantage, sProgress;
	private boolean flipped;

	private final float scale;

	public TrackProgress (float scale) {

		this.scale = scale;
		lblAdvantage = new HudLabel(scale, FontFace.CurseGreen, "", false, 2f);
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

	public void setPlayerSpeed (float mts) {
		speed = mts;
	}

	public void setTargetSpeed (float mts) {
		ghspeed = mts;
	}

	public void setPlayerDistance (float mt) {
// distPlayer = mt;
		distPlayer = AMath.lerp(prevDistPlayer, mt, 0.25f);
		prevDistPlayer = distPlayer;
	}

	public void setTargetDistance (float mt) {
// distGhost = mt;
		distGhost = AMath.lerp(prevDistGhost, mt, 0.25f);
		prevDistGhost = distGhost;
	}

	/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting line, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setPlayerProgression (float progress) {
		// smooth out high freq
		progressval = AMath.lerp(prevVal, progress, 0.25f);
		prevVal = progressval;
	}

	/** Sets the target's progression in the range [0,1] inclusive, to indicate car's track progress. 0 means on starting line, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setTargetProgression (float progress) {
		// smooth out high freq
		progressTargetVal = AMath.lerp(prevTargetVal, progress, 0.25f);
		prevTargetVal = progressTargetVal;
	}

	public void lapCompleted () {
	}

	public void tick () {
		lblAdvantage.tick();
	}

	public void render (SpriteBatch batch, float cameraZoom) {

		float playerToTarget = 0;

		if (distPlayer > 0) {
			if (!advantageShown) {
				advantageShown = true;
				lblAdvantage.queueShow(500);
				// Gdx.app.log("", "show");
			}

// float s = speed;
// if (distPlayer > distGhost && ghspeed > 0 && !useGhostSpeed) {
// useGhostSpeed = true;
// s = ghspeed;
// prevDistSecs = 0;
// } else if (distPlayer < distGhost && useGhostSpeed) {
// useGhostSpeed = false;
// prevDistSecs = 0;
// }
//
// float distSecs = AMath.lerp(prevDistSecs, (distPlayer - distGhost) / s, 0.1f);
// prevDistSecs = distSecs;
//
// lblAdvantage.setString(NumberString.format(distSecs), true);

			playerToTarget = AMath.fixup(progressval - progressTargetVal);
// Gdx.app.log("", "p2t=" + playerToTarget);

			if (distPlayer >= distGhost) {
				if (lblAdvantage.getFont() != FontFace.CurseGreenBig) {
					lblAdvantage.setFont(FontFace.CurseGreenBig);
				}
			}

			if (distPlayer < distGhost) {
				if (lblAdvantage.getFont() != FontFace.CurseRedBig) {
					lblAdvantage.setFont(FontFace.CurseRedBig);
				}
			}

			lblAdvantage.setString(NumberString.format(distPlayer - distGhost));

		} else if (advantageShown) {
			advantageShown = false;
			lblAdvantage.queueHide(1000);
			// Gdx.app.log("", "hide");
		}

		lblAdvantage.setScale(cameraZoom * 1f);
		lblAdvantage.setPosition(position.x, position.y - cameraZoom * Convert.scaledPixels(120));
		lblAdvantage.render(batch);

		float scl = cameraZoom * scale;

		// dbg
// dist = 0.35f;
// progressval = 0.5f;
// distGhost = 0.15f;
// distanceFromBest = 0.15f;

		float a = 1f;

		batch.setShader(shProgress);

		// set mask
		texMask.bind(1);
		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0);
		shProgress.setUniformi("u_texture1", 1);

		// player's progress
		shProgress.setUniformf("progress", progressval);
		sProgress.setColor(Color.WHITE);
		sProgress.setScale(scl);
		sProgress.setPosition(position.x - sProgress.getWidth() / 2, position.y - sProgress.getHeight() / 2);
		sProgress.draw(batch, a);
		batch.flush();

		// advantage/disadvantage
		float dist = MathUtils.clamp(playerToTarget * 8, -1, 1);

		boolean isBack = (dist < 0);
		if (isBack && !flipped) {
			flipped = true;
			sAdvantage.flip(true, false);
		} else if (!isBack && flipped) {
			flipped = false;
			sAdvantage.flip(true, false);
		}

		Color c = ColorUtils.paletteRYG(dist + 0.7f, 1f);
		shProgress.setUniformf("progress", Math.abs(playerToTarget));
		sAdvantage.setColor(c);
		sAdvantage.setScale(scl * 1.1f);
		sAdvantage.setPosition(position.x - sAdvantage.getWidth() / 2, position.y - sAdvantage.getHeight() / 2);
		sAdvantage.draw(batch, 1);
		batch.flush();

		batch.setShader(null);
	}
}
