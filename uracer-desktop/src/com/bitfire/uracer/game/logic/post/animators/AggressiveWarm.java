
package com.bitfire.uracer.game.logic.post.animators;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
import com.bitfire.postprocessing.effects.Curvature;
import com.bitfire.postprocessing.effects.Vignette;
import com.bitfire.postprocessing.effects.Zoomer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessingAnimator;
import com.bitfire.uracer.game.logic.types.CommonLogic;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;

public final class AggressiveWarm implements PostProcessingAnimator {
	public static final String Name = "AggressiveWarm";

	private boolean nightMode = false;
	private CommonLogic logic = null;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Curvature curvature = null;
	private PlayerCar player = null;
	private boolean hasPlayer = false;

	public AggressiveWarm (CommonLogic logic, PostProcessing post, boolean nightMode) {
		this.nightMode = nightMode;
		this.logic = logic;
		bloom = (Bloom)post.getEffect(PostProcessing.Effects.Bloom.name);
		zoom = (Zoomer)post.getEffect(PostProcessing.Effects.Zoomer.name);
		vignette = (Vignette)post.getEffect(PostProcessing.Effects.Vignette.name);
		crt = (CrtMonitor)post.getEffect(PostProcessing.Effects.Crt.name);
		curvature = (Curvature)post.getEffect(PostProcessing.Effects.Curvature.name);

		reset();
	}

	@Override
	public void setPlayer (PlayerCar player) {
		this.player = player;
		hasPlayer = (player != null);
	}

	@Override
	public void ErrorScreenShow (int milliseconds) {
	}

	@Override
	public void ErrorScreenHide (int milliseconds) {
	}

	@Override
	public void reset () {
		if (bloom != null) {
			float threshold = (nightMode ? 0.2f : 0.45f);
			Bloom.Settings bloomSettings = new Bloom.Settings("subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f,
				0.5f, 1f, 1.5f);
			bloom.setSettings(bloomSettings);
		}

		if (vignette != null) {
			vignette.setCoords(0.8f, 0.25f);
			vignette.setCenter(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
			vignette.setLutTexture(Art.postXpro);
			vignette.setLutIndexVal(0, 7);
			vignette.setLutIndexVal(1, 0);
			vignette.setLutIndexOffset(0);
			vignette.setEnabled(true);
		}

		if (crt != null) {
			startMs = TimeUtils.millis();
			crt.setTime(0);

			crt.setColorOffset(0.002f);
			crt.setDistortion(0.2f);
			crt.setZoom(0.9f);
			crt.setTint(0.95f, 0.8f, 1.0f);
		}

		if (curvature != null) {
			curvature.setDistortion(0.2f);
			curvature.setZoom(0.9f);
		}
	}

	private float prevDriftStrength = 0;
	private long startMs = 0;

	@Override
	public void update (float timeModFactor) {
		if (!hasPlayer) {
			return;
		}

		Vector2 playerScreenPos = GameRenderer.ScreenUtils.worldPxToScreen(player.state().position);

		float driftStrength = AMath.clamp(AMath.lerp(prevDriftStrength, player.driftState.driftStrength, 0.01f), 0, 1);
		prevDriftStrength = driftStrength;

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

		if (zoom != null && player != null) {
			float zoomfactor = timeModFactor;// * player.carState.currSpeedFactor;
			zoom.setOrigin(playerScreenPos);
			zoom.setBlurStrength(-0.1f * zoomfactor);
			zoom.setZoom(1.0f + 0.15f * zoomfactor);
		}

		if (bloom != null) {
			bloom.setBaseSaturation(AMath.lerp(1, 0.15f, timeModFactor));
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation(1f - timeModFactor * 0.5f);
			bloom.setThreshold(AMath.lerp(0.4f, 0.45f, timeModFactor));
		}

		if (vignette != null) {
			// vignette.setY( (1 - factor) * 0.74f + factor * 0.4f );

			if (vignette.controlSaturation) {
				// go with the "poor man"'s time dilation fx
				vignette.setSaturation(1f - timeModFactor * 0.55f);
				vignette.setSaturationMul(1f + timeModFactor * 0.125f);
			}

			// vignette.setCenter( playerScreenPos.x, playerScreenPos.y );
			// vignette.setCoords( 1.5f - driftStrength * 0.8f, 0.1f );

			vignette.setLutIntensity(timeModFactor * 1.25f);
			vignette.setIntensity(timeModFactor);
		}
	}
}
