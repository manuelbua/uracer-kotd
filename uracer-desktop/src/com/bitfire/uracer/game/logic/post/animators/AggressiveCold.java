
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

public final class AggressiveCold implements PostProcessingAnimator {
	public static final String Name = "AggressiveCold";

	private boolean nightMode = false;
	private CommonLogic logic = null;
	private Bloom bloom = null;
	private Zoomer zoom = null;
	private Vignette vignette = null;
	private CrtMonitor crt = null;
	private Curvature curvature = null;
	private PlayerCar player = null;
	private boolean hasPlayer = false;

	public AggressiveCold (CommonLogic logic, PostProcessing post, boolean nightMode) {
		this.logic = logic;
		this.nightMode = nightMode;
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
		reset();
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
			vignette.setCoords(0.85f, 0.3f);
			// vignette.setCoords( 1.5f, 0.1f );
			vignette.setCenter(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
			vignette.setLut(Art.postXpro);
			vignette.setLutIndex(5);
			vignette.setEnabled(true);
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

			crt.setColorOffset(0.002f);
			crt.setDistortion(0.125f);
			crt.setZoom(0.94f);

			// tv.setTint( 0.95f, 0.8f, 1.0f );
			crt.setTint(0.95f, 0.75f, 0.85f);
		}

		if (curvature != null) {
			float dist = 0.25f;
			curvature.setDistortion(dist);
			curvature.setZoom(1 - (dist / 2));

			// curvature.setDistortion( 0.125f );
			// curvature.setZoom( 0.94f );
		}
	}

	private float prevDriftStrength = 0;
	private long startMs = 0;
	Vector2 playerScreenPos = new Vector2();

	@Override
	public void update (float timeModFactor) {
		float driftStrength = 0;

		if (hasPlayer) {
			playerScreenPos.set(GameRenderer.ScreenUtils.worldPxToScreen(player.state().position));
			driftStrength = AMath.fixup(AMath.clamp(AMath.lerp(prevDriftStrength, player.driftState.driftStrength, 0.1f), 0, 1));
			prevDriftStrength = driftStrength;
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
			// boolean zoomEnabled = zoom.isEnabled();
			// if (AMath.isZero(timeModFactor) && zoomEnabled) {
			// zoom.setEnabled(false);
			// } else if (timeModFactor > 0 && !zoomEnabled) {
			// zoom.setEnabled(true);
			// }

			if (zoom.isEnabled()) {
				zoom.setOrigin(playerScreenPos);
				// zoom.setBlurStrength(-0.1f * driftStrength * timeModFactor);
				zoom.setBlurStrength(-0.1f * driftStrength * player.carState.currSpeedFactor);
			}
		}

		if (bloom != null) {
			// bloom.setBaseSaturation( 0.8f - timeFactor * 0.6f );
			bloom.setBaseSaturation(AMath.lerp(0.6f, 0.2f, timeModFactor));
			// bloom.setBloomSaturation( 1.5f - factor * 0.85f ); // TODO when charged
			// bloom.setBloomSaturation( 1.5f - factor * 1.5f ); // TODO when completely discharged
			bloom.setBloomSaturation(1f - timeModFactor * 0.8f);
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

			vignette.setLutIntensity(timeModFactor * 1.618f);// * AMath.clamp( driftStrength * 1.25f, 0, 1 ) );
			vignette.setIntensity(timeModFactor);
		}

		//
		// TODO out of dbg
		//
		if (curvature != null) {
			// curvature.setDistortion( player.carState.currSpeedFactor * 0.25f );
			// curvature.setZoom( 1 - 0.12f * player.carState.currSpeedFactor );

			float dist = 0.1618f;// * 0.75f;
			curvature.setDistortion(dist);
			curvature.setZoom(1 - (dist / 2));
		}

		if (crt != null) {
			float dist = 0.0f;
			dist = player.carState.currSpeedFactor * 0.1618f * 2f;
			crt.setDistortion(dist);
			crt.setZoom(1 - (dist / 2));
		}

		if (zoom.isEnabled()) {
			zoom.setOrigin(playerScreenPos);
			zoom.setBlurStrength(-0.10f);// * player.carState.currSpeedFactor);

		}
	}
}
