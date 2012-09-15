
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
	private float progressval, prevVal, speed;
	private float distanceFromBest, prevDist, distPlayer, distGhost;
	private boolean advantageShown;

	private final Texture texProgress, texMask;
	private final ShaderProgram shProgress;
	private final Sprite sAdvantage, sProgress;
	private boolean flipped;

	private final float offX, offY, w, h, scale;

	public TrackProgress (float scale) {

		this.scale = scale;
		lblAdvantage = new HudLabel(scale, FontFace.CurseGreen, "", false, 2f);
		advantageShown = false;
		lblAdvantage.setAlpha(0);

		texProgress = Art.texCircleProgress;
		texMask = Art.texCircleProgressMask;

		w = texProgress.getWidth();
		h = texProgress.getHeight();
		offX = w / 2;
		offY = h / 2;
		shProgress = ShaderLoader.fromFile("progress", "progress");

		sAdvantage = new Sprite(texProgress);
		sAdvantage.flip(false, true);
		flipped = false;

		sProgress = new Sprite(texProgress);
		sProgress.flip(false, true);
	}

	@Override
	public void dispose () {
		shProgress.dispose();
	}

	/** Sets the player's progression in the range [0,1] inclusive, to indicate player's track progress. 0 means on starting ine, 1
	 * means finished.
	 * @param progress The progress so far */
	public void setPlayerProgression (float progress) {
		// smooth out high freq
		progressval = AMath.lerp(prevVal, progress, 0.25f);
		prevVal = progressval;
	}

	/** Sets the player's advantage/disadvantage respect to the best replay, in normalized track space ([0,1]) */
	public void setDistanceFromBest (float distance) {
		// smooth out high freq
		distanceFromBest = AMath.lerp(prevDist, distance, 0.25f);
		prevDist = distanceFromBest;
	}

	public void setGhostDistance (float mt) {
		distGhost = mt;
	}

	public void setPlayerDistance (float mt) {
		distPlayer = mt;
	}

	public void setPlayerSpeed (float mts) {
		speed = mts;
	}

	public void lapCompleted () {
		prevDist = 0;
		distanceFromBest = 0;
	}

	public void tick () {
		lblAdvantage.tick();
	}

	public void render (SpriteBatch batch, float cameraZoom) {

		if (distPlayer > 0 && distGhost > 0 && speed > 15) {
			if (!advantageShown) {
				advantageShown = true;
				lblAdvantage.queueShow(500);
				// Gdx.app.log("", "show");
			}

			float distSecs = -(distPlayer - distGhost) / speed;
			lblAdvantage.setString(NumberString.format(distSecs), true);

			if (distSecs < 0 && lblAdvantage.getFont() != FontFace.CurseGreenBig) {
				lblAdvantage.setFont(FontFace.CurseGreenBig);
			} else if (distSecs > 0 && lblAdvantage.getFont() != FontFace.CurseRedBig) {
				lblAdvantage.setFont(FontFace.CurseRedBig);
			}

		} else if (advantageShown && speed < 15) {
			advantageShown = false;
			lblAdvantage.queueHide(1000);
			// Gdx.app.log("", "hide");
		}

		lblAdvantage.setScale(cameraZoom * 0.6f);
		lblAdvantage.setPosition(position.x, position.y - cameraZoom * Convert.scaledPixels(90));
		lblAdvantage.render(batch);

		float scl = cameraZoom * scale * 1.2f;
		float dist = MathUtils.clamp(distanceFromBest * 8, -1, 1);

		// dbg
// dist = 0.15f;
// progressval = 0.5f;
// distGhost = 0.15f;
// distanceFromBest = 0.15f;

		boolean isBack = (dist < 0);
		if (isBack && !flipped) {
			flipped = true;
			sAdvantage.flip(true, false);
		} else if (!isBack && flipped) {
			flipped = false;
			sAdvantage.flip(true, false);
		}

		float px = position.x - offX;
		float py = position.y - offY;
		float a = 0.8f;

		batch.setShader(shProgress);

		// set mask
		texMask.bind(1);
		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0);
		shProgress.setUniformi("u_texture1", 1);

		// player's progress
		shProgress.setUniformf("progress", progressval);
		sProgress.setColor(Color.WHITE);
		sProgress.setScale(scl);
		sProgress.setPosition(px, py);
		sProgress.draw(batch, a);
		batch.flush();

		// advantage/disadvantage
		if (distGhost > 0) {
			Color c = ColorUtils.paletteRYG(dist + 0.7f, 1);
			shProgress.setUniformf("progress", Math.abs(distanceFromBest * 4));
			sAdvantage.setColor(c);
			sAdvantage.setScale(scl * 1.2f);
			sAdvantage.setPosition(px, py);
			sAdvantage.draw(batch, a);
			batch.flush();
		}

		batch.setShader(null);
	}
}
