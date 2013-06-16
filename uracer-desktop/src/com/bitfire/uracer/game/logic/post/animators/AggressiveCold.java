
package com.bitfire.uracer.game.logic.post.animators;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Linear;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
import com.bitfire.postprocessing.effects.Curvature;
import com.bitfire.postprocessing.effects.Vignette;
import com.bitfire.postprocessing.effects.Zoomer;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessingAnimator;
import com.bitfire.uracer.game.logic.post.ssao.Ssao;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.ScaleUtils;

public final class AggressiveCold implements PostProcessingAnimator {
	public static final String Name = "AggressiveCold";

	private boolean nightMode = false;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Curvature curvature = null;
	private Ssao ssao = null;
	private PlayerCar player = null;
	private boolean hasPlayer = false;
	private BoxedFloat alertAmount, offsetAmount;
	private boolean alertBegan = false;
	private float bloomThreshold = 0.4f;

	private long startMs = 0;
	private Vector2 playerScreenPos = new Vector2();
	private InterpolatedFloat speed = new InterpolatedFloat();
	private InterpolatedFloat blurStrength = new InterpolatedFloat();

	public AggressiveCold (PostProcessing post, boolean nightMode) {
		this.nightMode = nightMode;
		bloom = (Bloom)post.getEffect(PostProcessing.Effects.Bloom.name);
		zoom = (Zoomer)post.getEffect(PostProcessing.Effects.Zoomer.name);
		vignette = (Vignette)post.getEffect(PostProcessing.Effects.Vignette.name);
		crt = (CrtMonitor)post.getEffect(PostProcessing.Effects.Crt.name);
		curvature = (Curvature)post.getEffect(PostProcessing.Effects.Curvature.name);
		ssao = (Ssao)post.getEffect(PostProcessing.Effects.Ssao.name);

		alertAmount = new BoxedFloat(0);
		offsetAmount = new BoxedFloat(0);
		blurStrength.setFixup(false);

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

			alertColorOffset(0.6f);
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

			GameTweener.stop(offsetAmount);
			seq = Timeline.createSequence();
			seq.push(Tween.to(offsetAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Quad.INOUT));
			GameTweener.start(seq);
		}
	}

	@Override
	public void alert (int milliseconds) {
		if (alertBegan) {
			return;
		}

		Timeline seq = Timeline.createSequence();

		//@off
		GameTweener.stop(alertAmount);
		seq
			.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, 75).target(0.75f).ease(Quad.IN))
			.pushPause(50)
			.push(Tween.to(alertAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Quad.OUT));
		GameTweener.start(seq);
		
//		Gdx.app.log("", "factor=" + factor);
		alertColorOffset(0);
		//@on

	}

	private void alertColorOffset (float to) {
		GameTweener.stop(offsetAmount);
		Timeline seq = Timeline.createSequence();
		seq.push(Tween.to(offsetAmount, BoxedFloatAccessor.VALUE, 150).target(0.75f).ease(Linear.INOUT)).pushPause(50)
			.push(Tween.to(offsetAmount, BoxedFloatAccessor.VALUE, 750).target(to).ease(Quad.OUT));

		GameTweener.start(seq);
	}

	@Override
	public void reset () {
		speed.reset(0, true);

		if (ssao != null) {
			ssao.setOcclusionThresholds(0.3f, 0.1f);
			ssao.setRadius(0.001f, nightMode ? 0.08f : 0.12f);
			ssao.setPower(nightMode ? 2f : 2f, 1);

			// if (Ssao.Quality.valueOf(UserPreferences.string(Preference.SsaoQuality)) == Ssao.Quality.High) {
			// ssao.setSampleCount(16);
			// ssao.setPatternSize(4);
			// } else {
			ssao.setSampleCount(nightMode ? 8 : 9);
			ssao.setPatternSize(nightMode ? 2 : 3);
			// }

			// ssao.enableDebug();
		}

		if (bloom != null) {
			bloomThreshold = (nightMode ? 0.22f : 0.4f);
			Bloom.Settings bloomSettings = new Bloom.Settings("subtle", Config.PostProcessing.BlurType,
				Config.PostProcessing.BlurNumPasses, 1.5f, bloomThreshold, 1f, 0.5f, 1f, 1.3f + (nightMode ? 0.2f : 0));
			bloom.setSettings(bloomSettings);
		}

		if (vignette != null) {
			vignette.setCoords(0.85f, 0.3f);
			// vignette.setCoords( 1.5f, 0.1f );
			vignette.setCenter(ScaleUtils.PlayWidth / 2, ScaleUtils.PlayHeight / 2);
			vignette.setLutTexture(Art.postXpro);
			vignette.setLutIndexVal(0, 16);
			vignette.setLutIndexVal(1, 7);
			vignette.setLutIndexOffset(0);
			vignette.setEnabled(true);

			// terminate pending, unfinished alert, if any
			if (alertAmount.value > 0) {
				alertBegan = true;
				alertEnds(Config.Graphics.DefaultResetFadeMilliseconds);
			}
		}

		if (zoom != null && hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
			zoom.setEnabled(true);
			zoom.setOrigin(playerScreenPos);
			zoom.setBlurStrength(0);
			blurStrength.reset(0, true);
		}

		if (crt != null) {
			startMs = TimeUtils.millis();
			crt.setTime(0);

			// note, a perfect color offset depends from screen size
			crt.setColorOffset(0.0005f);
			crt.setDistortion(0.125f);
			crt.setZoom(0.94f);

			// tv.setTint( 0.95f, 0.8f, 1.0f );
			crt.setTint(1, 1, 1);
		}

		if (curvature != null) {
			float dist = 0.25f;
			curvature.setDistortion(dist);
			curvature.setZoom(1 - (dist / 2));
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

	private void autoEnableEarthCurvature (float curvatureAmount) {
		boolean enabled = curvature.isEnabled();
		boolean isZero = AMath.isZero(curvatureAmount);

		if (isZero && enabled) {
			curvature.setEnabled(false);
		} else if (!isZero && !enabled) {
			curvature.setEnabled(true);
		}
	}

	@Override
	public void update (float zoomCamera, float warmUpCompletion) {
		float timeModFactor = URacer.Game.getTimeModFactor();

		// dbg
		// ssao.setSampleCount(16);
		// ssao.setPatternSize(4);
		// ssao.setPower(1, 2);
		// ssao.setRadius(0.001f, 0.2f);
		// ssao.setOcclusionThresholds(0.3f, 0.1f);
		// dbg

		if (hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
			speed.set(player.carState.currSpeedFactor, 0.02f);
		} else {
			playerScreenPos.set(0.5f, 0.5f);
		}

		if (crt != null) {
			// compute time (add noise)
			float secs = (float)(TimeUtils.millis() - startMs) / 1000;
			boolean randomNoiseInTime = false;
			if (randomNoiseInTime) {
				crt.setTime(secs + MathUtils.random() / (MathUtils.random() * 64f + 0.001f));
			} else {
				crt.setTime(secs);
			}
		}

		if (zoom != null) {

			if (hasPlayer) {
				float sfactor = speed.get();
				float z = (zoomCamera - (GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow));
				float v = (-0.09f * sfactor) - 0.09f * z;
				// Gdx.app.log("", "zoom=" + z);

				float strength = v + (-0.05f * timeModFactor * sfactor);
				blurStrength.set(strength, 1f);
			} else {
				blurStrength.set(0, 0.05f);
			}

			autoEnableZoomBlur(blurStrength.get());
			if (zoom.isEnabled()) {

				if (hasPlayer) {
					zoom.setOrigin(playerScreenPos);
				}

				zoom.setBlurStrength(blurStrength.get());
			}
		}

		if (bloom != null) {

			// float sat = 0.8f;
			// if (nightMode) sat += 0.2f;
			// sat *= warmUpCompletion;

			float bsat = 1.3f;
			if (nightMode) bsat += 0.2f;
			// bsat *= warmUpCompletion;

			// bloom.setBloomIntesity(1f);
			// bloom.setBloomSaturation(nightMode ? 0.8f : 2f);
			// bloom.setBaseIntesity(nightMode ? 0.7f : 1f);
			// bloom.setBaseSaturation(1f);

			// bloom.setBaseSaturation(AMath.lerp(sat, sat * 0.25f, timeModFactor));
			bloom.setBloomSaturation(AMath.lerp(bsat, bsat * 1.1f, timeModFactor));
			bloom.setBloomIntesity(1.4f);
			float t = 0.7f;
			bloom.setBaseSaturation(t - t * timeModFactor);
		}

		if (vignette != null) {
			if (vignette.controlSaturation) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation(1 - timeModFactor * 0.25f);
				vignette.setSaturationMul(1 + timeModFactor * 0.2f);
			}

			float lutIntensity = 0.1f + timeModFactor * 1f + alertAmount.value * 1f;
			lutIntensity = MathUtils.clamp(lutIntensity, 0, 1);

			vignette.setLutIntensity(lutIntensity);

			if (crt == null) {
				vignette.setIntensity(1.1f + 0.3f * timeModFactor);
			} else {
				vignette.setIntensity(0.7f);
			}

			vignette.setLutIndexOffset(alertAmount.value);
		}

		//
		// earth curvature (+ crt, optionally)
		//

		// maxzoom needs to be lowered of the same amount that the time dilation zoom feature will add to the zoom (0.1)
		// (see SinglePlayerLogic::updateCamera)
		float maxzoom = GameWorldRenderer.MaxCameraZoom - 0.1f;
		float factor = MathUtils.clamp(((zoomCamera - 1) / (maxzoom - 1)), 0, 1);

		float kdist = 0.18f;
		if (curvature != null) {
			float dist = kdist - kdist * factor;
			dist = AMath.fixup(dist);
			autoEnableEarthCurvature(dist);
			if (curvature.isEnabled()) {
				curvature.setDistortion(dist);
				curvature.setZoom(1 - (dist / 2));
			}
		}

		if (crt != null) {
			// color offset
			float amount = AMath.fixup(offsetAmount.value);
			crt.setColorOffset(0.0005f + 0.01f * amount);

			// zoom+earth curvature
			float dist = kdist - kdist * factor;
			dist = AMath.fixup(dist);
			crt.setDistortion(dist);
			crt.setZoom(1 - (dist / 2));
		}
	}
}
