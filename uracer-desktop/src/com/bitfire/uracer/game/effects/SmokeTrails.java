package com.bitfire.uracer.game.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.States;
import com.bitfire.uracer.game.entities.Car;
import com.bitfire.uracer.game.logic.DriftState;

public class SmokeTrails extends TrackEffect {
	private SmokeEffect fx[];
	private static final int SmokeEffectsCount = 1;
	public static final int MaxParticles = 100;

	private boolean isDrifting, wasDrifting;
	private DriftState drift;
	private Car player;

	private class SmokeEffect {
		private ParticleEffect effect;
		private ParticleEmitter baseEmitter;

		private final float MaxParticleLifeMinMs;
		private final float MaxParticleLifeMaxMs;
		private final float OriginalParticleScaling;
		private final float MaxParticlesPerEmitterPerSec;

		public SmokeEffect() {
			effect = new ParticleEffect();
			effect.load( Gdx.files.internal( "data/partfx/smoke.p" ), Art.carTextures );

			baseEmitter = effect.getEmitters().get( 0 );

			baseEmitter.setMaxParticleCount( MaxParticles );
			MaxParticleLifeMinMs = baseEmitter.getLife().getHighMin();
			MaxParticleLifeMaxMs = baseEmitter.getLife().getHighMax();
			OriginalParticleScaling = baseEmitter.getScale().getHighMax();
			MaxParticlesPerEmitterPerSec = baseEmitter.getEmission().getHighMax();

			baseEmitter.setAdditive( true );

			setScaleMul( 1f );
		}

		public void setLifeMul( float value ) {
			baseEmitter.getLife().setHighMin( MaxParticleLifeMinMs * value );
			baseEmitter.getLife().setHighMax( MaxParticleLifeMaxMs * value );
		}

		public void setScaleMul( float value ) {
			baseEmitter.getScale().setHigh( OriginalParticleScaling * value * GameData.scalingStrategy.invTileMapZoomFactor );
		}

		public void setEmissionMul( float value ) {
			baseEmitter.getEmission().setHigh( MaxParticlesPerEmitterPerSec * value );
		}

		public void start() {
			baseEmitter.start();
		}

		public void stop() {
			baseEmitter.allowCompletion();
		}

		public void reset() {
			stop();
		}

		public void render( SpriteBatch batch, float x, float y ) {
			effect.setPosition( x, y );
			effect.draw( batch, URacer.getLastDeltaSecs() );
		}

		public int getParticleCount() {
			int count = 0, max = effect.getEmitters().size;
			for( int i = 0; i < max; i++ ) {
				count += effect.getEmitters().get( i ).getActiveCount();
			}

			return count;
		}
	}

	public SmokeTrails( Car player ) {
		super( Type.SmokeTrails );

		fx = new SmokeEffect[ SmokeEffectsCount ];

		for( int i = 0; i < SmokeEffectsCount; i++ ) {
			fx[i] = new SmokeEffect();
			fx[i].setLifeMul( 2.25f );
			// fx[i].setScaleMul( .9f );
			fx[i].setEmissionMul( .8f );
			fx[i].stop();
		}

		this.player = player;
		isDrifting = wasDrifting = false;
		drift = States.driftState;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void onTick() {
		isDrifting = drift.isDrifting;

		if( isDrifting && !wasDrifting ) {
			// started drifting
			for( int i = 0; i < SmokeEffectsCount; i++ )
				fx[i].start();
		} else if( !isDrifting && wasDrifting ) {
			// ended drifting
			for( int i = 0; i < SmokeEffectsCount; i++ )
				fx[i].stop();
		}

		wasDrifting = isDrifting;

		// idx++;
		// if((idx&0x3f)==0)
		// {
		// System.out.println(this.getParticleCount());
		// }
	}

	private Vector2 tmp = new Vector2();

	@Override
	public void render( SpriteBatch batch ) {
		tmp.set( player.state().position.x, player.state().position.y );
		fx[0].render( batch, tmp.x, tmp.y );

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
	public void reset() {
		isDrifting = wasDrifting = false;
		for( int i = 0; i < SmokeEffectsCount; i++ )
			fx[i].reset();
	}

	@Override
	public int getParticleCount() {
		int count = 0;
		for( int i = 0; i < SmokeEffectsCount; i++ )
			count += fx[i].getParticleCount();

		return count;
	}

}
