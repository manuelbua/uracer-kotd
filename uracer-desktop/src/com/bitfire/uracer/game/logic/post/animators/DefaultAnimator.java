
package com.bitfire.uracer.game.logic.post.animators;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
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

public final class DefaultAnimator implements PostProcessingAnimator {
	private boolean nightMode = false;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Ssao ssao = null;
	private PlayerCar player = null;
	private boolean hasPlayer = false;
	private BoxedFloat alertAmount = new BoxedFloat(0);
	private boolean alertBegan = false;
	private float bloomThreshold = 0.4f;

	private long startMs = 0;
	private Vector2 playerScreenPos = new Vector2();
	private InterpolatedFloat speed = new InterpolatedFloat();
	private InterpolatedFloat blurStrength = new InterpolatedFloat();

	public DefaultAnimator (PostProcessing post, boolean nightMode) {
		this.nightMode = nightMode;
		bloom = (Bloom)post.getEffect(PostProcessing.Effects.Bloom.name);
		zoom = (Zoomer)post.getEffect(PostProcessing.Effects.Zoomer.name);
		vignette = (Vignette)post.getEffect(PostProcessing.Effects.Vignette.name);
		crt = (CrtMonitor)post.getEffect(PostProcessing.Effects.Crt.name);
		ssao = (Ssao)post.getEffect(PostProcessing.Effects.Ssao.name);
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
	public void reset () {
		speed.reset(0, true);

		if (ssao != null) {
			ssao.setOcclusionThresholds(0.3f, 0.1f);
			ssao.setRadius(0.001f, nightMode ? 0.08f : 0.12f);
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
			// special effects palette on slot #1
			vignette.setLutIndexVal(0, 16);
			vignette.setLutIndexVal(1, 12);
			vignette.setLutIndexOffset(0);
			vignette.setEnabled(true);
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

			// note: a perfect color offset depends on screen size
			crt.setColorOffset(0.0005f);
			crt.setChromaticDispersion(0.112f, 0.112f);
			crt.setDistortion(0.125f);
			crt.setZoom(0.94f);

			// tv.setTint( 0.95f, 0.8f, 1.0f );
			crt.setTint(1, 1, 1);
		}

		//
		// reset composed effects
		//

		// terminate pending, unfinished alert, if any
		if (alertAmount.value > 0) {
			alertBegan = true;
			alertEnds(Config.Graphics.DefaultResetFadeMilliseconds);
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

	@Override
	public void update (float zoomCamera, float warmUpCompletion, float collisionFactor) {
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
			speed.set(player.carState.currSpeedFactor, 0.25f);
		} else {
			playerScreenPos.set(0.5f, 0.5f);
		}

		float cf = collisionFactor;
		// cf = 1f;

		if (crt != null) {
			// compute time (add noise)
			float secs = (float)(TimeUtils.millis() - startMs) / 1000;
			boolean randomNoiseInTime = false;
			if (randomNoiseInTime) {
				crt.setTime(secs + MathUtils.random() / (MathUtils.random() * 64f + 0.001f));
			} else {
				crt.setTime(secs);
			}

			float factor = MathUtils.clamp(((zoomCamera - 1) / GameWorldRenderer.ZoomRange), 0, 1);
			float kdist = 0.20f;
			float dist = kdist - kdist * factor;

			// modulates color offset by collision factor)
			// crt.setColorOffset(MathUtils.clamp(0.025f * cf, 0, 0.008f));

			float amount = MathUtils.clamp(cf + 0.14f, 0, 1) * -0.8f;
			// float amount = MathUtils.clamp(cf + 0.1f, 0, 1) * -1.6f;
			amount -= 0.15f * AMath.fixup(factor - kdist);

			// Gdx.app.log("", "" + amount);
			crt.setChromaticDispersion(amount, amount);
			// crt.setChromaticDispersion(0f, 0f);

			// zoom+earth curvature
			dist = AMath.fixup(dist);
			crt.setDistortion(dist);
			crt.setZoom(1 - (dist / 2));
		}

		if (zoom != null) {
			if (hasPlayer) {
				float sfactor = speed.get();
				float z = (zoomCamera - (GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow));
				float v = (-0.09f * sfactor) - 0.09f * z - 0.3f * cf;
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

		float bsat = 0, sat = 0;
		if (bloom != null) {
			float intensity = 1.4f + 4f * cf + (nightMode ? 4 * cf : 0);
			bloom.setBloomIntesity(intensity);

			bsat = 1f;
			if (vignette != null) {
				bsat += 0.2f * timeModFactor;
			} else {
				bsat += 0.2f;
			}

			if (nightMode) bsat += 0.4f;
			bsat *= 1 - cf * 3f;

			sat = 0.7f;
			sat = sat - sat * timeModFactor;
			sat = sat * (1 - cf);
			sat = AMath.lerp(sat, -0.25f, MathUtils.clamp(alertAmount.value * 2, 0, 1));
			sat = AMath.lerp(sat, -0.25f, cf);

			sat = MathUtils.clamp(sat, 0, 1);
			bsat = MathUtils.clamp(bsat, 0, 1);
			bloom.setBaseSaturation(sat);
			bloom.setBloomSaturation(bsat);
			// bloom.setBaseSaturation(1);
			// bloom.setBloomSaturation(1);

			Gdx.app.log("", "base=" + sat + ", bloom=" + bsat);
		}

		if (vignette != null) {
			float lutIntensity = MathUtils.clamp(0.15f + timeModFactor * 1 + alertAmount.value * 1 + cf * 1, 0, 1);
			float offset = MathUtils.clamp(cf * 3 + alertAmount.value, 0, 1);
			vignette.setLutIntensity(lutIntensity);
			vignette.setLutIndexOffset(offset);
			// vignette.setLutIndexVal(0, 16);
			// vignette.setLutIndexVal(1, 12);
		}
	}
}
