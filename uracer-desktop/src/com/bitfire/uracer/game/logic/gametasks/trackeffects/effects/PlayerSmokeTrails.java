
package com.bitfire.uracer.game.logic.gametasks.trackeffects.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Art;

/** FIXME disabled for a long time, need testing again
 * 
 * @author bmanuel */
public class PlayerSmokeTrails extends TrackEffect {
	public static final int MaxParticles = 1000;

	private SmokeEffect fx[];
	private static final int SmokeEffectsCount = 1;
	private PlayerCar player;
	private boolean isDrifting, wasDrifting;
	private ScalingStrategy scalingStrategy;
	private float posX, posY;

	private class SmokeEffect {
		private ParticleEffect effect;
		private ParticleEmitter baseEmitter;

		private final float MaxParticleLifeMinMs;
		private final float MaxParticleLifeMaxMs;
		private final float OriginalParticleScaling;
		private final float MaxParticlesPerEmitterPerSec;

		public SmokeEffect () {
			effect = new ParticleEffect();
			effect.load(Gdx.files.internal("data/partfx/smoke.p"), Art.cars);

			baseEmitter = effect.getEmitters().get(0);

			baseEmitter.setMaxParticleCount(MaxParticles);
			MaxParticleLifeMinMs = baseEmitter.getLife().getHighMin();
			MaxParticleLifeMaxMs = baseEmitter.getLife().getHighMax();
			OriginalParticleScaling = baseEmitter.getScale().getHighMax();
			MaxParticlesPerEmitterPerSec = baseEmitter.getEmission().getHighMax();
		}

		public void setLifeMul (float value) {
			baseEmitter.getLife().setHighMin(MaxParticleLifeMinMs * value);
			baseEmitter.getLife().setHighMax(MaxParticleLifeMaxMs * value);
		}

		public final void setScaleMul (float value) {
			baseEmitter.getScale().setHigh(OriginalParticleScaling * value * scalingStrategy.invTileMapZoomFactor);
		}

		public void setEmissionMul (float value) {
			baseEmitter.getEmission().setHigh(MaxParticlesPerEmitterPerSec * value);
		}

		public void start () {
			baseEmitter.start();
		}

		public void stop () {
			baseEmitter.allowCompletion();
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

	public PlayerSmokeTrails (ScalingStrategy scalingStrategy, PlayerCar player) {
		super(Type.CarSmokeTrails);
		this.player = player;
		this.scalingStrategy = scalingStrategy;

		fx = new SmokeEffect[SmokeEffectsCount];

		for (int i = 0; i < SmokeEffectsCount; i++) {
			fx[i] = new SmokeEffect();
			fx[i].setLifeMul(3.5f);
			fx[i].setScaleMul(1.5f);
			fx[i].setEmissionMul(0.25f);
// fx[i].stop();
// fx[i].start();
		}

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

		if (isDrifting && !wasDrifting) {
			// started drifting
			for (int i = 0; i < SmokeEffectsCount; i++) {
				fx[i].start();
				Gdx.app.log("", "start smoke trails");
			}
		} else if ((!isDrifting && wasDrifting)) {
			// ended drifting
			for (int i = 0; i < SmokeEffectsCount; i++) {
				fx[i].stop();
				Gdx.app.log("", "stop smoke trails");
			}
		}

		wasDrifting = isDrifting;
		setPosition(player.state().position.x, player.state().position.y);
	}

	private Vector2 tmp = new Vector2();

	@Override
	public void render (SpriteBatch batch) {
		tmp.set(posX, posY);

		fx[0].baseEmitter.setAdditive(true);
		fx[0].setLifeMul(MathUtils.random(10f, 15f) * player.driftState.driftStrength);
		fx[0].setScaleMul(MathUtils.random(0.1f, 0.1f + 10f * player.driftState.driftStrength));

		float t = player.driftState.driftStrength * 0.75f;
		fx[0].baseEmitter.getTransparency().setHighMin(t);
		fx[0].baseEmitter.getTransparency().setHighMax(t);

		float[] colors = fx[0].baseEmitter.getTint().getColors();
		float v = 0.2f;
// float r = 0.5f;
// float g = 0.5f;
// float b = 0.5f;
		colors[0] = v * player.driftState.driftStrength;
		colors[1] = v * player.driftState.driftStrength;
		colors[2] = v * player.driftState.driftStrength;

// Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_DST_ALPHA);
		fx[0].render(batch, tmp.x, tmp.y);

// Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		// // rear left
		// fx[0].render( batch, tmp.x - 10, tmp.y - 10 );
		//
		// // rear right
		// fx[1].render( batch, tmp.x - 10, tmp.y + 10 );
		//
		// // front left
		// fx[2].render( batch, tmp.x + 10, tmp.y - 10 );
		//
		// // front right
		// fx[3].render( batch, tmp.x + 10, tmp.y + 10 );
	}

	@Override
	public void reset () {
		isDrifting = false;
		wasDrifting = false;
		for (int i = 0; i < SmokeEffectsCount; i++) {
			fx[i].reset();
		}
	}

	@Override
	public int getMaxParticleCount () {
		return MaxParticles * SmokeEffectsCount;
	}

	@Override
	public int getParticleCount () {
		int count = 0;
		for (int i = 0; i < SmokeEffectsCount; i++) {
			count += fx[i].getParticleCount();
		}

		return count;
	}

}
