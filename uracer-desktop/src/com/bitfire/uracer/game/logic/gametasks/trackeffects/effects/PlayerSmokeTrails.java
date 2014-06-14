
package com.bitfire.uracer.game.logic.gametasks.trackeffects.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffectType;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;

public class PlayerSmokeTrails extends TrackEffect {
	public static final int MaxParticles = 1000;

	private SmokeEffect fx;
	private static final int SmokeEffectsCount = 1;
	private Vector2 position = new Vector2();

	public PlayerSmokeTrails () {
		super(TrackEffectType.CarSmokeTrails);
		fx = new SmokeEffect(this);
		fx.setMaxParticleCount(MaxParticles);
	}

	@Override
	public void dispose () {
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);
		if (!hasPlayer) {
			fx.effect.allowCompletion();
		} else {
			fx.effect.reset();
		}
	}

	@Override
	public void tick () {
		if (hasPlayer) {
			if (fx.effect.isComplete()) {
				fx.start();
			}
		}
	}

	@Override
	public void render (SpriteBatch batch) {
		if (hasPlayer) {

			float dfactor = player.driftState.driftStrength;
			float sfactor = player.carState.currSpeedFactor;

			fx.setLifeMul(2f);
			fx.setScaleMul(1f + 20f * dfactor * sfactor);

			float t = 0.5f * dfactor;
			fx.baseEmitter.getTransparency().setHighMin(t);
			fx.baseEmitter.getTransparency().setHighMax(t);

			float[] colors = fx.baseEmitter.getTint().getColors();
			float v = 0.3f;
			colors[0] = v * dfactor;
			colors[1] = v * dfactor;
			colors[2] = v * dfactor;

			float r = 0.7f;
			float g = 0.8f;
			float b = 1f;
			if (MathUtils.randomBoolean()) {
				r = 0.7f;
			}

			float colorscale = 0.15f + 0.3f * dfactor;
			r *= colorscale;
			g *= colorscale;
			b *= colorscale;
			colors[0] = r;
			colors[1] = g;
			colors[2] = b;
			position.set(player.state().position);
		}

		fx.render(batch, position.x, position.y);
	}

	@Override
	public void reset () {
		// fx.reset();
	}

	@Override
	public int getMaxParticleCount () {
		return MaxParticles * SmokeEffectsCount;
	}

	@Override
	public int getParticleCount () {
		return fx.getParticleCount();
	}

	private static final class SmokeEffect {
		private TrackEffect owner;
		protected ParticleEffect effect;
		private ParticleEmitter baseEmitter;

		private final float MaxParticleLifeMinMs;
		private final float MaxParticleLifeMaxMs;
		private final float OriginalParticleScaling;

		// private final float MaxParticlesPerEmitterPerSec;

		public SmokeEffect (TrackEffect owner) {
			this.owner = owner;
			effect = new ParticleEffect();
			effect.load(Gdx.files.internal("data/partfx/smoke-small.p"), Art.particles, "");

			baseEmitter = effect.getEmitters().get(0);

			MaxParticleLifeMinMs = baseEmitter.getLife().getHighMin();
			MaxParticleLifeMaxMs = baseEmitter.getLife().getHighMax();
			OriginalParticleScaling = baseEmitter.getScale().getHighMax();
			// MaxParticlesPerEmitterPerSec = baseEmitter.getEmission().getHighMax();

			effect.start();
		}

		public void setLifeMul (float value) {
			baseEmitter.getLife().setHighMin(MaxParticleLifeMinMs * value);
			baseEmitter.getLife().setHighMax(MaxParticleLifeMaxMs * value);
		}

		public void setMaxParticleCount (int value) {
			baseEmitter.setMaxParticleCount(value);
		}

		public final void setScaleMul (float value) {
			baseEmitter.getScale().setHigh(OriginalParticleScaling * value);
		}

		// public void setEmissionMul (float value) {
		// baseEmitter.getEmission().setHigh(MaxParticlesPerEmitterPerSec * value);
		// }

		public void start () {
			for (int i = 0; i < effect.getEmitters().size; i++) {
				effect.getEmitters().get(i).setContinuous(true);
				effect.getEmitters().get(i).start();
			}
		}

		// public void stop () {
		// for (int i = 0; i < effect.getEmitters().size; i++) {
		// effect.getEmitters().get(i).allowCompletion();
		// }
		// }

		// public void reset () {
		// stop();
		// }

		public void render (SpriteBatch batch, float x, float y) {
			float delta = owner.isPaused() ? 0 : URacer.Game.getLastDeltaSecs() * URacer.timeMultiplier;
			effect.setPosition(x, y);
			effect.draw(batch, delta);
		}

		public int getParticleCount () {
			int count = 0, max = effect.getEmitters().size;
			for (int i = 0; i < max; i++) {
				count += effect.getEmitters().get(i).getActiveCount();
			}

			return count;
		}
	}
}
