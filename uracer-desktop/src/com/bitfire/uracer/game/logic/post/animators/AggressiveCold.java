
package com.bitfire.uracer.game.logic.post.animators;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.WindowedMean;
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
import com.bitfire.uracer.game.logic.types.CommonLogic;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

public final class AggressiveCold implements PostProcessingAnimator {
	public static final String Name = "AggressiveCold";

	private boolean nightMode = false;
	private CommonLogic logic = null;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Curvature curvature = null;
	private Ssao ssao = null;
	private PlayerCar player = null;
	private boolean hasPlayer = false;
	private BoxedFloat wrongWayAmount;
	private boolean wrongWayBegan = false;
	private boolean alertCollision = false;
	private float lastCollisionFactor = 0;
	private float bloomThreshold = 0.45f;

	public AggressiveCold (CommonLogic logic, PostProcessing post, boolean nightMode) {
		this.logic = logic;
		this.nightMode = nightMode;
		bloom = (Bloom)post.getEffect(PostProcessing.Effects.Bloom.name);
		zoom = (Zoomer)post.getEffect(PostProcessing.Effects.Zoomer.name);
		vignette = (Vignette)post.getEffect(PostProcessing.Effects.Vignette.name);
		crt = (CrtMonitor)post.getEffect(PostProcessing.Effects.Crt.name);
		curvature = (Curvature)post.getEffect(PostProcessing.Effects.Curvature.name);
		ssao = (Ssao)post.getEffect(PostProcessing.Effects.Ssao.name);

		wrongWayAmount = new BoxedFloat(0);

		reset();
	}

	@Override
	public void setPlayer (PlayerCar player) {
		this.player = player;
		hasPlayer = (player != null);
		reset();
	}

	@Override
	public void alertWrongWayBegins (int milliseconds) {
		if (!wrongWayBegan) {
			wrongWayBegan = true;
			alertCollision = false;
			lastCollisionFactor = 0;
			GameTweener.stop(wrongWayAmount);
			Timeline seq = Timeline.createSequence();

			//@off
		seq
			.push(Tween.to(wrongWayAmount, BoxedFloatAccessor.VALUE, milliseconds).target(1.5f).ease(Quad.IN))
			.pushPause(50)
			.push(Tween.to(wrongWayAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0.75f).ease(Quad.OUT))
		;
		//@on

			GameTweener.start(seq);
		}
	}

	@Override
	public void alertWrongWayEnds (int milliseconds) {
		if (wrongWayBegan) {
			wrongWayBegan = false;
			GameTweener.stop(wrongWayAmount);
			Timeline seq = Timeline.createSequence();
			seq.push(Tween.to(wrongWayAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Quad.INOUT));
			GameTweener.start(seq);
		}
	}

	@Override
	public void alertCollision (float factor, int milliseconds) {
// if (wrongWayBegan || alertCollision) {
		if (wrongWayBegan) {
			lastCollisionFactor = 0;
			return;
		}

		// DO NOT accept subsequent collision alerts if the factor
		// is LOWER than the alert currently being shown
		if (factor < lastCollisionFactor && alertCollision) {
			return;
		}

		lastCollisionFactor = factor;
		alertCollision = true;
		GameTweener.stop(wrongWayAmount);
		Timeline seq = Timeline.createSequence();

		factor = MathUtils.clamp(factor, 0, 1);

		//@off
		seq
			.push(Tween.to(wrongWayAmount, BoxedFloatAccessor.VALUE, 75).target(factor).ease(Quad.IN))
			.pushPause(50)
			.push(Tween.to(wrongWayAmount, BoxedFloatAccessor.VALUE, milliseconds).target(0).ease(Quad.OUT))
			.setCallback(new TweenCallback() {
				
				@Override
				public void onEvent (int type, BaseTween<?> source) {
					switch (type) {
					case COMPLETE:
						alertCollision = false;
						lastCollisionFactor = 0;
					}
				}
			})
		;
		//@on

		GameTweener.start(seq);
	}

	@Override
	public void reset () {
		alertCollision = false;

		if (ssao != null) {
			ssao.setOcclusionThresholds(0.3f, 0.1f);
			ssao.setRadius(0.001f, nightMode ? 0.08f : 0.12f);
			ssao.setPower(nightMode ? 3f : 2f, 1);
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
			bloomThreshold = (nightMode ? 0.3f : 0.45f);
			Bloom.Settings bloomSettings = new Bloom.Settings("subtle", Config.PostProcessing.BlurType,
				Config.PostProcessing.BlurNumPasses, 1.5f, bloomThreshold, 1f, 0.5f, 1f, 1.5f);
			bloom.setSettings(bloomSettings);
		}

		if (vignette != null) {
			vignette.setCoords(0.85f, 0.3f);
			// vignette.setCoords( 1.5f, 0.1f );
			vignette.setCenter(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
			vignette.setLutTexture(Art.postXpro);
			vignette.setLutIndexVal(0, 16);
			vignette.setLutIndexVal(1, 7);
			vignette.setLutIndexOffset(0);
			lastCollisionFactor = 0;
			vignette.setEnabled(true);

			if (wrongWayAmount.value > 0) {
				wrongWayBegan = true;
				alertWrongWayEnds(Config.Graphics.DefaultResetFadeMilliseconds);
			}
		}

		if (zoom != null && hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
			zoom.setEnabled(true);
			zoom.setOrigin(playerScreenPos);
			zoom.setBlurStrength(0);
		}

		if (crt != null) {
			startMs = TimeUtils.millis();
			crt.setTime(0);

			// note, a perfect color offset depends from screen size
			crt.setColorOffset(0.002f);
			crt.setDistortion(0.125f);
			crt.setZoom(0.94f);

			// tv.setTint( 0.95f, 0.8f, 1.0f );
			crt.setTint(0.9f, 0.8f, 0.85f);
		}

		if (curvature != null) {
			float dist = 0.25f;
			curvature.setDistortion(dist);
			curvature.setZoom(1 - (dist / 2));

			// curvature.setDistortion( 0.125f );
			// curvature.setZoom( 0.94f );
		}
	}

	private long startMs = 0;
	Vector2 playerScreenPos = new Vector2();
	private WindowedMean meanSpeed = new WindowedMean(2);
	private WindowedMean meanStrength = new WindowedMean(5);

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
	public void update (float zoomCamera) {
		float timeModFactor = URacer.Game.getTimeModFactor();
		float currDriftStrength = 0;
		float currSpeedFactor = 0;

		if (hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));

			meanStrength.addValue(player.driftState.driftStrength);
			meanSpeed.addValue(player.carState.currSpeedFactor);

			currDriftStrength = AMath.fixup(AMath.clamp(meanStrength.getMean(), 0, 1));
			currSpeedFactor = AMath.fixup(AMath.clamp(meanSpeed.getMean(), 0, 1));
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

		if (zoom != null && hasPlayer) {
			// auto-disable zoom
			// float blurStrength = -0.1f * timeModFactor * currSpeedFactor;
			float blurStrength = (-0.035f - 0.09f * currSpeedFactor) * timeModFactor - 0.02f * currSpeedFactor;
			// float blurStrength = (-0.035f - 0.09f * currSpeedFactor) * timeModFactor;

			autoEnableZoomBlur(blurStrength);

			if (zoom.isEnabled()) {
				zoom.setOrigin(playerScreenPos);
				zoom.setBlurStrength(blurStrength);
			}

		}

		if (bloom != null) {

			bloom.setBaseSaturation(AMath.lerp(0.6f, 0.4f, timeModFactor));
			// bloom.setBaseSaturation(AMath.lerp(0.8f, 0.05f, timeModFactor));
			// bloom.setBloomSaturation(1.5f - timeModFactor * 0.15f);
			// if (!nightMode) {
			// bloom.setThreshold(0.3f);
			// }

			// with RttRatio = 0.5f
			// bloom.setBaseIntesity(0.9f);
			// bloom.setBaseSaturation(1f);
			// bloom.setBloomIntesity(1f);
			// bloom.setBloomSaturation(1f);
			// bloom.setThreshold(0.4f);
			// bloom.setBlurPasses(2);
		}

		if (vignette != null) {
			if (vignette.controlSaturation) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation(1 - timeModFactor * 0.25f);
				vignette.setSaturationMul(1 + timeModFactor * 0.2f);
			}

			// if (player != null) {
			// vignette.setCenter( playerScreenPos.x, playerScreenPos.y );
			// vignette.setCenter( Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2 );
			// }

			float lutIntensity = 0.15f + timeModFactor * 0.85f + wrongWayAmount.value * 0.85f;
			lutIntensity = MathUtils.clamp(lutIntensity, 0, 1);

			vignette.setLutIntensity(lutIntensity);
			if (crt == null) {
				vignette.setIntensity(0.8f + 0.3f * timeModFactor);
			} else {
				vignette.setIntensity(0.7f);
			}
			vignette.setLutIndexOffset(wrongWayAmount.value);
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
			float dist = kdist - kdist * factor;
			dist = AMath.fixup(dist);
			crt.setDistortion(dist);
			crt.setZoom(1 - (dist / 2));
		}

	}
}
