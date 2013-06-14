
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
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.LapCompletionMonitorEvent;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.utils.ShaderLoader;

public class TrackProgress extends Positionable {
	static final float Smoothing = 0.25f;
	private HudLabel lblAdvantage;
	private boolean lblAdvantageShown;

	private final Texture texMask;
	private final ShaderProgram shProgress;
	private final Sprite sprAdvantage, sprProgress;
	private boolean flipped, hasTarget, isCurrentLapValid, isWarmUp, hasLapStarted;
	private float playerToTarget;

	private String customMessage = "";
	private TrackProgressData data = new TrackProgressData();

	/** Data needed by this component */
	private class TrackProgressData {

		InterpolatedFloat playerDistance, targetDistance;
		InterpolatedFloat playerProgress, playerProgressAdv, targetProgress;

		public TrackProgressData () {
			playerDistance = new InterpolatedFloat();
			targetDistance = new InterpolatedFloat();
			playerProgress = new InterpolatedFloat();
			playerProgressAdv = new InterpolatedFloat();
			targetProgress = new InterpolatedFloat();
		}

		public void reset (boolean resetState) {
			playerDistance.reset(0, resetState);
			targetDistance.reset(0, resetState);
			playerProgress.reset(0, resetState);
			playerProgressAdv.reset(0, resetState);
			targetProgress.reset(0, resetState);
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

		GameEvents.lapCompletion.addListener(lapMonitor, LapCompletionMonitorEvent.Type.onLapStarted);
	}

	@Override
	public void dispose () {
		shProgress.dispose();
		GameEvents.lapCompletion.removeListener(lapMonitor, LapCompletionMonitorEvent.Type.onLapStarted);
	}

	public void resetData (boolean resetState) {
		data.reset(resetState);
	}

	private LapCompletionMonitorEvent.Listener lapMonitor = new LapCompletionMonitorEvent.Listener() {
		@SuppressWarnings("incomplete-switch")
		@Override
		public void handle (Object source, LapCompletionMonitorEvent.Type type, LapCompletionMonitorEvent.Order order) {
			switch (type) {
			case onLapStarted:
				hasLapStarted = true;
				break;
			}
		};
	};

	public void update (boolean isWarmUp, boolean isCurrentLapValid, GameTrack gameTrack, PlayerCar player, GhostCar target) {
		this.isCurrentLapValid = isCurrentLapValid;
		this.isWarmUp = isWarmUp;

		boolean hadTarget = hasTarget;
		hasTarget = (target != null);
		playerToTarget = 0;

		if (isWarmUp) {
			data.reset(true);

			if (isCurrentLapValid) {
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
			if (isCurrentLapValid) {
				customMessage = "";
				data.playerProgress.set(gameTrack.getTrackCompletion(player), Smoothing);
				data.playerProgressAdv.set(gameTrack.getTrackCompletion(player), Smoothing);
				data.playerDistance.set(gameTrack.getTrackDistance(player, 0), Smoothing);

				if (hasTarget) {
					data.targetDistance.set(gameTrack.getTrackDistance(target, 0), Smoothing);
					data.targetProgress.set(gameTrack.getTrackCompletion(target), Smoothing);

					// In case the player didn't have a target but just got one now, the track progress
					// meter shall be reset as well as its state since we don't want the advantage/disadvantage bar making its
					// first-time appearance with an animation from full-progress towards start-line progress.
					// In all other cases the state is preserved.
					if (hasLapStarted && !hadTarget /* hasTarget is implicit! */) {
						hasLapStarted = false;
						data.playerProgressAdv.reset(0, true);
						data.targetProgress.reset(0, true);
						data.playerDistance.reset(0, true);
						data.targetDistance.reset(0, true);
					}

					playerToTarget = AMath.fixup(data.playerProgressAdv.get() - data.targetProgress.get());
					// Gdx.app.log("", "" + playerToTarget);
				}
			} else {
				customMessage = "Press \"R\"\nto restart";
				data.reset(true);
			}
		}

		// playerToTarget = 0f;
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

		if (isCurrentLapValid) {
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
			if (hasTarget && !isWarmUp) {
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
}
