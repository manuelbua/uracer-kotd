
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.utils.ShaderLoader;

public class TrackProgress extends Positionable {
	private HudLabel lblAdvantage;
	private boolean lblAdvantageShown;

	private final Texture texMask;
	private final ShaderProgram shProgress;
	private final Sprite sprAdvantage, sprProgress;
	private boolean flipped;

	private String customMessage = "";
	private TrackProgressData data = null;
	private boolean hasTarget;

	// smooth animation between end-of-track (n meters) and start-of-track (0 meters)
	private InterpolatedFloat playerProgress = new InterpolatedFloat();
	private InterpolatedFloat playerToTarget = new InterpolatedFloat();

	public TrackProgress () {
		lblAdvantage = new HudLabel(FontFace.CurseWhiteBig, "", false);
		lblAdvantageShown = false;
		lblAdvantage.setAlpha(0);

		texMask = Art.texCircleProgressMask;

		shProgress = ShaderLoader.fromFile("progress", "progress");

		sprAdvantage = new Sprite(Art.texCircleProgress);
		sprAdvantage.flip(false, true);
		flipped = false;

		sprProgress = new Sprite(Art.texRadLinesProgress);
		sprProgress.flip(false, true);
	}

	public void setTrackProgressData (TrackProgressData data) {
		this.data = data;
	}

	@Override
	public void dispose () {
		shProgress.dispose();
	}

	public void resetPlayerToTarget () {
		playerToTarget.reset(true);
	}

	public void update (GameTrack gameTrack, PlayerCar player, GhostCar target) {
		hasTarget = (target != null);

		if (data.isWarmUp) {
			if (data.isCurrentLapValid) {
				int metersToRace = Math.round(gameTrack.getTotalLength() - gameTrack.getTrackDistance(player, 0));
				if (metersToRace > 0) {
					customMessage = "Start in " + metersToRace + " mt";
				} else {
					customMessage = "Started!";
				}
			} else {
				customMessage = "Press \"R\"\nto restart";
			}
		} else {
			if (data.isCurrentLapValid) {
				customMessage = "";
			} else {
				customMessage = "Press \"R\"\nto restart";
			}
		}
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
		// advantage/disadvantage
		float timeFactor = URacer.Game.getTimeModFactor() * 0.3f;

		// advantage if > 0, disadvantage if < 0
		float dist = MathUtils.clamp(data.playerToTarget, -1, 1);
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
				lblAdvantage.setString(String.format("%.02f", v) + " m", false);
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

		if (data.isCurrentLapValid) {
			float scl = cameraZoom * scale * (1f + timeFactor);
			batch.setShader(shProgress);

			// set mask
			texMask.bind(1);
			Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
			shProgress.setUniformi("u_texture1", 1);

			scl += .07f * URacer.Game.getTimeModFactor();

			// player's track progress
			playerProgress.set(data.playerProgress.get(), 0.125f);

			shProgress.setUniformf("progress", playerProgress.get());
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
			if (hasTarget && !data.isWarmUp) {
				playerToTarget.set(Math.abs(data.playerToTarget), 0.125f);
				// Gdx.app.log("", "p2t=" + data.playerToTarget + ", my=" + playerToTarget.get());

				shProgress.setUniformf("progress", playerToTarget.get());
				sprAdvantage.setColor(advantageColor);
				sprAdvantage.setScale(scl * 1.1f);
				sprAdvantage.setPosition(position.x - sprAdvantage.getWidth() / 2, position.y - sprAdvantage.getHeight() / 2);
				sprAdvantage.draw(batch, 1);
				batch.flush();
			}

			batch.setShader(null);
		}
	}
}
