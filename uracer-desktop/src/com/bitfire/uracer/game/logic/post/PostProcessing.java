
package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.postprocessing.PostProcessorEffect;
import com.bitfire.postprocessing.effects.Bloom;
import com.bitfire.postprocessing.effects.CrtMonitor;
import com.bitfire.postprocessing.effects.Curvature;
import com.bitfire.postprocessing.effects.Vignette;
import com.bitfire.postprocessing.effects.Zoomer;
import com.bitfire.postprocessing.filters.RadialBlur;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.logic.post.ssao.Ssao;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.utils.Hash;

/** Encapsulates a post-processor animator that manages effects such as bloom and zoomblur to compose and enhance the gaming
 * experience. */
public final class PostProcessing {

	public enum Effects {
		Zoomer, Bloom, Vignette, Crt, Curvature, Ssao, MotionBlur;

		public String name;

		private Effects () {
			name = this.toString();
		}
	}

	private boolean hasPostProcessor;
	private final PostProcessor postProcessor;
	private boolean isNightMode;

	// public access to stored effects
	public LongMap<PostProcessorEffect> effects = new LongMap<PostProcessorEffect>();

	// animators
	public LongMap<PostProcessingAnimator> animators = new LongMap<PostProcessingAnimator>();
	private PostProcessingAnimator currentAnimator;
	private GameRenderer gameRenderer;

	public PostProcessing (GameWorld gameWorld, GameRenderer gameRenderer) {
		this.gameRenderer = gameRenderer;
		this.postProcessor = gameRenderer.getPostProcessor();
		hasPostProcessor = (this.postProcessor != null);
		this.isNightMode = gameWorld.isNightMode();

		if (hasPostProcessor) {
			configurePostProcessor(postProcessor);
			currentAnimator = null;
		}
	}

	/** Creates the effects that will be available to the animators/manipulators to use, remember that the ownership of the
	 * instantiated objects is transfered to the PostProcessor when adding the effect to it. */
	private void configurePostProcessor (PostProcessor postProcessor) {
		postProcessor.setEnabled(true);
		postProcessor.setClearBits(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		postProcessor.setClearDepth(1f);
		postProcessor.setBufferTextureWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
		gameRenderer.disableNormalDepthMap();

		if (UserPreferences.bool(Preference.Ssao)) {
			addEffect(Effects.Ssao.name, new Ssao(Ssao.Quality.valueOf(UserPreferences.string(Preference.SsaoQuality)), isNightMode));
			gameRenderer.enableNormalDepthMap();
		}

		// addEffect(Effects.MotionBlur.name, new CameraMotion());

		if (UserPreferences.bool(Preference.ZoomRadialBlur)) {
			RadialBlur.Quality rbq = RadialBlur.Quality.valueOf(UserPreferences.string(Preference.ZoomRadialBlurQuality));
			Zoomer z = (UserPreferences.bool(Preference.ZoomRadialBlur) ? new Zoomer(rbq) : new Zoomer());
			z.setBlurStrength(0);
			z.setZoom(1);
			addEffect(Effects.Zoomer.name, z);
		}

		if (UserPreferences.bool(Preference.Bloom)) {
			addEffect(Effects.Bloom.name, new Bloom(Config.PostProcessing.BloomFboWidth, Config.PostProcessing.BloomFboHeight));
		}

		if (UserPreferences.bool(Preference.Vignetting)) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			addEffect(Effects.Vignette.name, new Vignette(!UserPreferences.bool(Preference.Bloom)));
		}

		if (UserPreferences.bool(Preference.CrtScreen)) {
			addEffect(Effects.Crt.name, new CrtMonitor(UserPreferences.bool(Preference.EarthCurvature), false));
		} else if (UserPreferences.bool(Preference.EarthCurvature)) {
			addEffect(Effects.Curvature.name, new Curvature());
		}

		Gdx.app.log("PostProcessing", "Post-processing enabled and configured");
	}

	public void addEffect (String name, PostProcessorEffect effect) {
		if (hasPostProcessor) {
			postProcessor.addEffect(effect);
			effects.put(Hash.APHash(name), effect);
		}
	}

	public PostProcessorEffect getEffect (String name) {
		return effects.get(Hash.APHash(name));
	}

	public boolean hasEffect (String name) {
		return (effects.get(Hash.APHash(name)) != null);
	}

	public void addAnimator (String name, PostProcessingAnimator animator) {
		animators.put(Hash.APHash(name), animator);
	}

	public boolean hasAnimator (String name) {
		return (animators.get(Hash.APHash(name)) != null);
	}

	public PostProcessingAnimator getAnimator (String name) {
		return animators.get(Hash.APHash(name));
	}

	public void enableAnimator (String name) {
		if (!hasPostProcessor) {
			return;
		}

		PostProcessingAnimator next = animators.get(Hash.APHash(name));
		if (next != null) {
			currentAnimator = next;
			currentAnimator.reset();
		}
	}

	public void disableAnimator () {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.reset();
			currentAnimator = null;
		}
	}

	public void resetAnimator () {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.reset();
		}
	}

	public void onBeforeRender (float zoom, float warmUpCompletion) {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.update(zoom, warmUpCompletion);
		}
	}

	public void setPlayer (PlayerCar player) {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.setPlayer(player);
		}
	}

	// features
	public void alertWrongWayBegins (int milliseconds) {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.alertWrongWayBegins(milliseconds);
		}
	}

	public void alertWrongWayEnds (int milliseconds) {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.alertWrongWayEnds(milliseconds);
		}
	}

	public void alertCollision (float factor, int milliseconds) {
		if (hasPostProcessor && currentAnimator != null) {
			currentAnimator.alertCollision(factor, milliseconds);
		}
	}
}
