
package com.bitfire.uracer.game.logic.post.animators;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Linear;
import aurelienribon.tweenengine.equations.Quad;
import box2dLight.PointLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
import com.bitfire.postprocessing.effects.Vignette;
import com.bitfire.postprocessing.effects.Zoomer;
import com.bitfire.postprocessing.filters.Combine;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessingAnimator;
import com.bitfire.uracer.game.logic.post.lightshafts.LightShafts;
import com.bitfire.uracer.game.logic.post.ssao.Ssao;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.ScaleUtils;

public final class DefaultAnimator implements PostProcessingAnimator {
	private GameWorld world;
	private boolean nightMode = false;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Ssao ssao = null;
	private LightShafts shafts = null;
	private PlayerCar player = null;
	private boolean hasPlayer = false;
	private BoxedFloat alertAmount = new BoxedFloat(0);
	private BoxedFloat pauseAmount = new BoxedFloat(0);
	private boolean alertBegan = false, pauseBegan = false;
	private float bloomThreshold = 0.4f;

	private long startMs = 0;
	private Vector2 playerScreenPos = new Vector2();
	// private Vector2 cameraScreenPos = new Vector2();
	private InterpolatedFloat speed = new InterpolatedFloat();
	private InterpolatedFloat zoomBlurStrengthFactor = new InterpolatedFloat();

	public DefaultAnimator (PostProcessing post, GameWorld gameWorld) {
		this.world = gameWorld;
		this.nightMode = gameWorld.isNightMode();
		bloom = (Bloom)post.getEffect(PostProcessing.Effects.Bloom.name);
		zoom = (Zoomer)post.getEffect(PostProcessing.Effects.Zoomer.name);
		vignette = (Vignette)post.getEffect(PostProcessing.Effects.Vignette.name);
		crt = (CrtMonitor)post.getEffect(PostProcessing.Effects.Crt.name);
		ssao = (Ssao)post.getEffect(PostProcessing.Effects.Ssao.name);
		shafts = (LightShafts)post.getEffect(PostProcessing.Effects.LightShafts.name);
		zoomBlurStrengthFactor.setFixup(false);
		reset();
	}

	@Override
	public void setPlayer (PlayerCar player) {
		this.player = player;
		hasPlayer = (player != null);
		reset();
	}

	@Override
	public void alertBegins (int milliseconds) {
		if (!alertBegan) {
			alertBegan = true;
			GameTweener.stop(alertAmount);
			Timeline seq = Timeline.createSequence();

			//@off
			seq
				.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, milliseconds).target(1.5f).ease(Quad.IN))
				.pushPause(50)
				.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0.75f).ease(Quad.OUT))
			;
			GameTweener.start(seq);
			//@on
		}
	}

	@Override
	public void alertEnds (int milliseconds) {
		if (alertBegan) {
			alertBegan = false;

			GameTweener.stop(alertAmount);
			Timeline seq = Timeline.createSequence();
			seq.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Quad.INOUT));
			GameTweener.start(seq);
		}
	}

	@Override
	public void alert (int milliseconds) {
		if (alertBegan) {
			return;
		}

		//@off
		Timeline seq = Timeline.createSequence();
		GameTweener.stop(alertAmount);
		seq
			.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, 75).target(0.75f).ease(Quad.IN))
			.pushPause(50)
			.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Quad.OUT));
		GameTweener.start(seq);
		//@on
	}

	@Override
	public void gamePause (int milliseconds) {
		if (!pauseBegan) {
			pauseBegan = true;
			SysTweener.stop(pauseAmount);
			Timeline seq = Timeline.createSequence();

			//@off
			seq
				.push(Tween.to(pauseAmount, BoxedFloatAccessor.VALUE, milliseconds).target(1f).ease(Linear.INOUT))
			;
			SysTweener.start(seq);
			//@on
		}
	}

	@Override
	public void gameResume (int milliseconds) {
		if (pauseBegan) {
			pauseBegan = false;

			SysTweener.stop(pauseAmount);
			Timeline seq = Timeline.createSequence();
			seq.push(Tween.to(pauseAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Linear.INOUT));
			SysTweener.start(seq);
		}
	}

	@Override
	public void reset () {
		speed.reset(0, true);

		if (ssao != null) {
			ssao.setOcclusionThresholds(0.3f, 0.1f);
			// ssao.setRadius(0.001f, nightMode ? 0.08f : 0.12f);
			ssao.setRadius(0.001f, 0.12f);
			ssao.setPower(nightMode ? 2f : 2f, 2);
			ssao.setSampleCount(nightMode ? 8 : 9);
			ssao.setPatternSize(nightMode ? 2 : 3);
		}

		if (bloom != null) {
			bloomThreshold = (nightMode ? 0.2f : 0.4f);
			Bloom.Settings bloomSettings = new Bloom.Settings("subtle", Config.PostProcessing.BlurType,
				Config.PostProcessing.BlurNumPasses, 1.5f, bloomThreshold, 1f, 0.5f, 1f, 1.3f + (nightMode ? 0.2f : 0));
			bloom.setSettings(bloomSettings);
		}

		if (vignette != null) {
			vignette.setCoords(0.85f, 0.3f);
			vignette.setIntensity(1);
			vignette.setCenter(ScaleUtils.PlayWidth / 2, ScaleUtils.PlayHeight / 2);
			vignette.setLutTexture(Art.postXpro);

			// setup palettes
			// default aspect to slot #0
			// damage effects palette on slot #1
			// vignette.setLutIndexVal(0, 16);
			vignette.setLutIndexVal(0, 16);
			vignette.setLutIndexVal(1, 7);
			vignette.setLutIndexOffset(0);
			vignette.setEnabled(true);
		}

		if (zoom != null && hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
			zoom.setEnabled(true);
			zoom.setOrigin(playerScreenPos);
			zoom.setBlurStrength(0);
			zoom.setZoom(1);
			zoomBlurStrengthFactor.reset(0, true);
		}

		if (crt != null) {
			startMs = TimeUtils.millis();
			crt.setTime(0);

			// note: a perfect color offset depends on screen size
			crt.setColorOffset(0.0005f);
			crt.setChromaticDispersion(0.112f, 0.112f);
			crt.setDistortion(0.125f);
			crt.setZoom(0.94f);

			// tv.setTint( 0.95f, 0.8f, 1.0f );
			crt.setTint(1, 1, 1);
			crt.getCombinePass().setSource2Intensity(1f);
		}

		if (shafts != null) {
			shafts.setThreshold(0.65f);
			shafts.setParams(24, 0.05f, 0.92f, 0.84f, 3.65f, 1f, 0, 0);
			shafts.setLightScreenPositionN(0.5f, 0.5f);
		}

		//
		// reset composed effects
		//

		// terminate pending, unfinished alert, if any
		if (alertAmount.value > 0) {
			alertBegan = true;
			alertEnds(Config.Graphics.DefaultResetFadeMilliseconds);
		}

		// terminate pending, unfinished alert, if any
		if (pauseAmount.value > 0) {
			pauseBegan = true;
			gameResume(Config.Graphics.DefaultResetFadeMilliseconds);
		}
	}

	private void autoEnableZoomBlur (float blurStrength) {
		boolean enabled = zoom.isEnabled();
		boolean isZero = AMath.isZero(blurStrength);

		if (isZero && enabled) {
			zoom.setEnabled(false);
		} else if (!isZero && !enabled) {
			zoom.setEnabled(true);
		}
	}

	private void updateLights (TrackProgressData progressData, Color ambient, Color trees, float collisionFactor) {
		float base = 0.1f;
		float timeModFactor = URacer.Game.getTimeModFactor();

		//@off
		ambient.set(
			base * 1.5f + collisionFactor * 0.5f,
			base,
			base + base * 3f * timeModFactor,
			0.55f + 0.05f * timeModFactor
		);
		//@on

		ambient.clamp();
		trees.set(ambient);

		// Gdx.app.log("", "" + ambient);

		// update point lights, more intensity from lights near the player
		PlayerCar player = world.getPlayer();
		PointLight[] lights = world.getLights();
		if (lights != null && player != null) {
			for (int l = 0; l < lights.length; l++) {
				float dist = player.getWorldPosMt().dst2(lights[l].getPosition());
				float maxdist = 30;
				maxdist *= maxdist;
				dist = 1 - MathUtils.clamp(dist, 0, maxdist) / maxdist;
				float r = 1f;
				float g = 1f;
				float b = 1f;
				float a = 0.65f;
				lights[l].setColor(r, g, b, a);// + AMath.fixup(0.4f * dist));
			}
		}
	}

	@Override
	public void update (Vector2 cameraPos, TrackProgressData progressData, Color ambient, Color trees, float zoomCamera,
		float warmUpCompletion, float collisionFactor, boolean paused) {
		float timeModFactor = URacer.Game.getTimeModFactor();

		// dbg
		// ssao.setSampleCount(16);
		// ssao.setPatternSize(4);
		// ssao.setPower(1, 2);
		// ssao.setRadius(0.001f, 0.2f);
		// ssao.setOcclusionThresholds(0.3f, 0.1f);
		// dbg

		// compute needed screen positions
		if (hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
			speed.set(player.carState.currSpeedFactor, 0.25f);
		} else {
			playerScreenPos.set(0.5f, 0.5f);
		}

		// cameraScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(cameraPos));
		// Gdx.app.log("", "campos=" + cameraPos);

		float cf = collisionFactor;
		// cf = 1f;

		updateLights(progressData, ambient, trees, cf);

		if (crt != null) {
			if (!paused) {
				// compute time (add noise)
				float secs = (float)(TimeUtils.millis() - startMs) / 1000;
				// boolean randomNoiseInTime = false;
				// if (randomNoiseInTime) {
				// crt.setTime(secs + MathUtils.random() / (MathUtils.random() * 64f + 0.001f));
				// } else {
				crt.setTime(secs);
				// }
			}

			// needed variables
			float curvature_factor = MathUtils.clamp(((zoomCamera - 1) / GameWorldRenderer.ZoomRange), 0, 1);
			float kdist = 0.20f;

			// modulates color offset by collision factor)
			float amount = MathUtils.clamp(cf + 0.14f, 0, 1) * -0.8f;
			amount -= 0.15f * AMath.fixup(curvature_factor - kdist);

			// amount = 0.2f + cf * 5f;

			// Gdx.app.log("", "" + amount);
			crt.setChromaticDispersion(amount, amount);

			// earth curvature
			float dist = kdist - kdist * curvature_factor;
			dist = AMath.fixup(dist);
			crt.setDistortion(dist);
			crt.setZoom(1 - (dist / 2));
		}

		if (zoom != null) {
			if (hasPlayer) {
				float sfactor = speed.get();
				float strength = -0.05f * timeModFactor + (-0.05f * sfactor) - 0.4f * cf;
				// Gdx.app.log("", "strength=" + strength);
				zoomBlurStrengthFactor.set(strength, 1f);
			} else {
				zoomBlurStrengthFactor.set(0, 0.05f);
			}

			float f = zoomBlurStrengthFactor.get();
			// Gdx.app.log("", "f=" + f);

			autoEnableZoomBlur(f);
			if (zoom.isEnabled()) {
				zoom.setBlurStrength(f);
				if (hasPlayer) {
					zoom.setOrigin(playerScreenPos);
				}
			}
		}

		if (shafts != null) {
			// final float ScaleMt = 10;
			// final float InvScaleMt = 1f / ScaleMt;
			// float pdist = 0;
			// // // to normalized range
			// if (progressData.hasTarget) {
			// pdist = MathUtils.clamp(progressData.playerDistance.get() - progressData.targetDistance.get(), 0, ScaleMt);
			// pdist *= InvScaleMt;
			// }

			// float sfactor = speed.get();
			// float lim_min = 0.7f;
			// float lim_max = 0.8f;
			// float th = lim_max - (lim_max - lim_min) * pdist;

			// Gdx.app.log("", "pdist=" + pdist);

			shafts.setThreshold(0.9f);
			shafts.setDensity(0.84f);
			shafts.setExposure(0.08f);
			shafts.setWeight(1);
			shafts.setDecay(1f);
			Combine combine = shafts.getCombinePass();
			combine.setSource2Intensity(1f);
			combine.setSource2Saturation(1f);

			// Gdx.app.log("", "speed=" + sfactor);
			// shafts.setLightScreenPositionN(0.5f, 0.3f);
		}

		float bsat = 0f, sat = 0f;
		if (bloom != null) {
			// float intensity = 1.4f + 4f * cf;// + (nightMode ? 4f * cf : 0f);
			float intensity = 1f + 2f * cf + 2 * pauseAmount.value;// + (nightMode ? 4f * cf : 0f);
			// Gdx.app.log("", "bloom intensity=" + intensity);
			bloom.setBloomIntesity(intensity);

			bsat = 1f;
			// bsat += 0.2f * timeModFactor;

			// if (nightMode) bsat += 0.0f;
			bsat *= (1f - (cf * 1f));

			// float progress = MathUtils.clamp(progressData.playerToTarget, 0, 1) * 3f;
			sat = 0.7f + (nightMode ? 0.6f : 0);// + progress;
			// sat = sat * (1 - timeModFactor);
			sat = sat * (1f - cf);
			sat = AMath.lerp(sat, -0.25f, MathUtils.clamp(alertAmount.value * 2f, 0f, 1f));
			sat = AMath.lerp(sat, -0.25f, cf);

			sat = MathUtils.clamp(sat, 0f, 3f);
			bsat = MathUtils.clamp(bsat, 0f, 3f);

			bloom.setBaseSaturation(sat * (1 - pauseAmount.value));
			bloom.setBloomSaturation(bsat * (1 - pauseAmount.value));

			// bloom.setBaseSaturation(0);
			// bloom.setBloomSaturation(0);

			// float bloomTh = AMath.lerp(bloomThreshold, bloomThreshold - 0.01f, timeModFactor);
			// bloom.setThreshold(bloomTh);
			// Gdx.app.log("", "Bth=" + bloomTh);

			// bloom.setBaseSaturation(1);
			// bloom.setBloomSaturation(1);
			// Gdx.app.log("", "sat=" + sat + ", bsat=" + bsat);
		}

		// cf = 1;
		if (vignette != null) {
			float lutIntensity = MathUtils.clamp(1f + timeModFactor * 0.75f + alertAmount.value * 1 + cf * 1, 0, 1.5f);
			float offset = MathUtils.clamp(cf * 3 + alertAmount.value /* + timeModFactor * 0.25f */, 0, 1);
			vignette.setLutIntensity(lutIntensity);
			vignette.setLutIndexOffset(offset);

			// vignette.setLutIntensity(1);
			// vignette.setLutIndexVal(0, 13);
			// vignette.setLutIndexVal(1, 7);

			// Gdx.app.log("", "li=" + lutIntensity + ", lo=" + offset);
		}
	}
}
