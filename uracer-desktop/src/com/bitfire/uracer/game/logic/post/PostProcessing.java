
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
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.game.logic.post.animators.AggressiveCold;
import com.bitfire.uracer.game.logic.post.ssao.Ssao;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.ScaleUtils;
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

	private boolean hasPostProcessor = false;
	private PostProcessor postProcessor = null;
	private boolean needNormalDepthMap = false;

	// public access to stored effects
	public LongMap<PostProcessorEffect> effects = new LongMap<PostProcessorEffect>();

	// animators
	private PostProcessingAnimator animator = null;
	private boolean hasAnimator = false;

	public PostProcessing (GameWorld gameWorld) {

		// post-processing
		if (UserPreferences.bool(Preference.PostProcessing)) {
			postProcessor = new PostProcessor(ScaleUtils.PlayViewport, true /* depth */, false /* alpha */, URacer.Game.isDesktop() /* supports32Bpp */);
			PostProcessor.EnableQueryStates = false;
			postProcessor.setClearBits(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			postProcessor.setClearColor(0, 0, 0, 1);
			postProcessor.setClearDepth(1);
			postProcessor.setEnabled(true);
			postProcessor.setBufferTextureWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
			hasPostProcessor = true;
			createEffects();
			setAnimator(new AggressiveCold(this, gameWorld.isNightMode()));
		}
	}

	public void dispose () {
		if (hasPostProcessor) {
			postProcessor.dispose();
		}
	}

	/** Creates the effects that will be available to the animators/manipulators to use, remember that the ownership of the
	 * instantiated objects is transfered to the PostProcessor when adding the effect to it. */
	private void createEffects () {
		if (UserPreferences.bool(Preference.Ssao)) {
			addEffect(
				Effects.Ssao.name,
				new Ssao(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, Ssao.Quality.valueOf(UserPreferences
					.string(Preference.SsaoQuality))));
			needNormalDepthMap = true;
		}

		// addEffect(Effects.MotionBlur.name, new CameraMotion());
		int refW = Config.Graphics.ReferenceScreenWidth;
		int refH = Config.Graphics.ReferenceScreenHeight;

		if (UserPreferences.bool(Preference.ZoomRadialBlur)) {
			RadialBlur.Quality rbq = RadialBlur.Quality.valueOf(UserPreferences.string(Preference.ZoomRadialBlurQuality));
			Zoomer z = new Zoomer(refW, refH, rbq);
			z.setBlurStrength(0);
			z.setZoom(1);
			addEffect(Effects.Zoomer.name, z);
		}

		if (UserPreferences.bool(Preference.Bloom)) {
			int fboW = (int)((float)ScaleUtils.PlayWidth * Config.PostProcessing.FboRatio);
			int fboH = (int)((float)ScaleUtils.PlayHeight * Config.PostProcessing.FboRatio);
			addEffect(Effects.Bloom.name, new Bloom(fboW, fboH));
		}

		if (UserPreferences.bool(Preference.Vignetting)) {
			// if there is no bloom, let's control the final saturation via
			// the vignette filter
			addEffect(Effects.Vignette.name,
				new Vignette(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, !UserPreferences.bool(Preference.Bloom)));
		}

		if (UserPreferences.bool(Preference.CrtScreen)) {
			addEffect(Effects.Crt.name,
				new CrtMonitor(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, UserPreferences.bool(Preference.EarthCurvature), false));
		} else if (UserPreferences.bool(Preference.EarthCurvature)) {
			addEffect(Effects.Curvature.name, new Curvature());
		}

		Gdx.app.log("PostProcessing", "Post-processing enabled and configured");
	}

	private void setAnimator (PostProcessingAnimator animator) {
		hasAnimator = (animator != null);
		this.animator = animator;
		animator.reset();
	}

	public boolean requiresNormalDepthMap () {
		return needNormalDepthMap;
	}

	public boolean isEnabled () {
		return hasPostProcessor;
	}

	public PostProcessor getPostProcessor () {
		return postProcessor;
	}

	public void addEffect (String name, PostProcessorEffect effect) {
		if (hasPostProcessor) {
			postProcessor.addEffect(effect);
			effects.put(Hash.APHash(name), effect);
		}
	}

	public PostProcessorEffect getEffect (String name) {
		if (hasPostProcessor) {
			return effects.get(Hash.APHash(name));
		}

		return null;
	}

	public boolean hasEffect (String name) {
		if (hasPostProcessor) {
			return (effects.get(Hash.APHash(name)) != null);
		}

		return false;
	}

	public void resetAnimator () {
		if (hasPostProcessor && hasAnimator) {
			animator.reset();
		}
	}

	public void onBeforeRender (float zoom, float warmUpCompletion) {
		if (hasPostProcessor && hasAnimator) {
			animator.update(zoom, warmUpCompletion);
		}
	}

	public void setPlayer (PlayerCar player) {
		if (hasPostProcessor && hasAnimator) {
			animator.setPlayer(player);
		}
	}

	// features
	public void alertWrongWayBegins (int milliseconds) {
		if (hasPostProcessor && hasAnimator) {
			animator.alertWrongWayBegins(milliseconds);
		}
	}

	public void alertWrongWayEnds (int milliseconds) {
		if (hasPostProcessor && hasAnimator) {
			animator.alertWrongWayEnds(milliseconds);
		}
	}

	public void alertCollision (float collisionFactor, int milliseconds) {
		if (hasPostProcessor && hasAnimator) {
			animator.alertCollision(collisionFactor, milliseconds);
		}
	}
}
