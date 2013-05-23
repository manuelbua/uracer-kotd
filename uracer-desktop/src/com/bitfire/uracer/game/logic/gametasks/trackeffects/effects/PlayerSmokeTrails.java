
package com.bitfire.uracer.game.logic.gametasks.trackeffects.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffectType;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;

/** FIXME disabled for a long time, need testing again
 * 
 * @author bmanuel */
public class PlayerSmokeTrails extends TrackEffect {
	public static final int MaxParticles = 1000;

	private SmokeEffect fx;
	private static final int SmokeEffectsCount = 1;
	private PlayerCar player;
	private boolean isDrifting, wasDrifting;
	private float posX, posY;

	public PlayerSmokeTrails (PlayerCar player) {
		super(TrackEffectType.CarSmokeTrails);
		this.player = player;

		fx = new SmokeEffect();
		fx.setMaxParticleCount(MaxParticles);
		// fx.start();

		isDrifting = false;
		wasDrifting = false;
		posX = 0;
		posY = 0;
	}

	public void setPosition (float x, float y) {
		posX = x;
		posY = y;
	}

	@Override
	public void dispose () {
	}

	@Override
	public void tick () {
		isDrifting = player.driftState.isDrifting && player.driftState.driftStrength > 0f;

		if (fx.effect.isComplete()) {
			fx.start();
		}

		// if (player.driftState.driftStrength > 0) {
		// for (int i = 0; i < SmokeEffectsCount; i++) {
		// // fx[i].setMaxParticleCount(MaxParticles);
		// fx[i].start();
		// }
		// }

		// if (isDrifting && !wasDrifting) {
		// // started drifting
		// for (int i = 0; i < SmokeEffectsCount; i++) {
		// fx[i].start();
		// // Gdx.app.log("", "start smoke trails");
		// }
		// } else if ((!isDrifting && wasDrifting)) {
		// // ended drifting
		// for (int i = 0; i < SmokeEffectsCount; i++) {
		// fx[i].stop();
		// // Gdx.app.log("", "stop smoke trails");
		// }
		// }

		wasDrifting = isDrifting;
		setPosition(player.state().position.x, player.state().position.y);
	}

	@Override
	public void render (SpriteBatch batch) {
		float dfactor = player.driftState.driftStrength;
		float sfactor = player.carState.currSpeedFactor;

		fx.setLifeMul(2f);
		fx.setScaleMul(1f + 20f * dfactor * sfactor);

		float t = 1f * dfactor;
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

		fx.render(batch, posX, posY);
	}

	@Override
	public void reset () {
		isDrifting = false;
		wasDrifting = false;
		fx.reset();
	}

	@Override
	public int getMaxParticleCount () {
		return MaxParticles * SmokeEffectsCount;
	}

	@Override
	public int getParticleCount () {
		return fx.getParticleCount();
	}

	private class SmokeEffect {
		protected ParticleEffect effect;
		private ParticleEmitter baseEmitter;

		private final float MaxParticleLifeMinMs;
		private final float MaxParticleLifeMaxMs;
		private final float OriginalParticleScaling;
		private final float MaxParticlesPerEmitterPerSec;

		public SmokeEffect () {
			effect = new ParticleEffect();
			effect.load(Gdx.files.internal("data/partfx/smoke-small.p"), Art.particles);

			baseEmitter = effect.getEmitters().get(0);

			MaxParticleLifeMinMs = baseEmitter.getLife().getHighMin();
			MaxParticleLifeMaxMs = baseEmitter.getLife().getHighMax();
			OriginalParticleScaling = baseEmitter.getScale().getHighMax();
			MaxParticlesPerEmitterPerSec = baseEmitter.getEmission().getHighMax();

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

		public void setEmissionMul (float value) {
			baseEmitter.getEmission().setHigh(MaxParticlesPerEmitterPerSec * value);
		}

		public void start () {
			for (int i = 0; i < effect.getEmitters().size; i++) {
				effect.getEmitters().get(i).setContinuous(true);
				effect.getEmitters().get(i).start();
			}
		}

		public void stop () {
			for (int i = 0; i < effect.getEmitters().size; i++) {
				effect.getEmitters().get(i).allowCompletion();
			}
		}

		public void reset () {
			stop();
		}

		public void render (SpriteBatch batch, float x, float y) {
			effect.setPosition(x, y);
			effect.draw(batch, URacer.Game.getLastDeltaSecs() * URacer.timeMultiplier);
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
