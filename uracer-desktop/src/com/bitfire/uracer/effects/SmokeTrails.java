package com.bitfire.uracer.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.entities.vehicles.Car;

public class SmokeTrails extends TrackEffect
{
	ParticleEffect effect;
	ParticleEmitter baseEmitter;
	ParticleEmitter[] emitters;
	private int emitterIdx;

	private final int MaxEmitters = 10;
	private final int MaxParticlesPerEmitter = 5;

	// private boolean isDrifting, wasDrifting;
	// private DriftInfo drift;
	private Car player;

	public SmokeTrails( Car player )
	{
		super( Effects.SmokeTrails );
		effect = new ParticleEffect();
		effect.load( Gdx.files.internal( "data/partfx/smoke.p" ), Gdx.files.internal( "data/partfx" ) );

		baseEmitter = effect.getEmitters().get( 0 );
		baseEmitter.setMinParticleCount( 0 );
		baseEmitter.setMaxParticleCount( 0 );
		baseEmitter.setAdditive( false );
		baseEmitter.reset();

		emitters = new ParticleEmitter[ MaxEmitters ];
		emitters[0] = baseEmitter;
		for( int i = 1; i < MaxEmitters; i++ )
		{
			emitters[i] = new ParticleEmitter( baseEmitter );
			emitters[i].reset();

			effect.getEmitters().add( emitters[i] );
		}

		effect.start();

		emitterIdx = 0;
		this.player = player;
		// isDrifting = wasDrifting = false;
		// drift = DriftInfo.get();
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public void tick()
	{
		// isDrifting = drift.isDrifting;
		// if( isDrifting && !wasDrifting )
		// {
		// // started drifting
		// effect.start();
		// } else if( !isDrifting && wasDrifting )
		// {
		// // ended drifting
		// effect.allowCompletion();
		// }

		if( !effect.isComplete() )
		{
			effect.setPosition( player.state().position.x, player.state().position.y );
			effect.update( Physics.dt );
		}

		// wasDrifting = isDrifting;
		System.out.println( "active-count=" + getParticleCount() );
	}

	@Override
	public void render( SpriteBatch batch )
	{
		if( !effect.isComplete() )
		{
			effect.draw( batch );
		}
	}

	@Override
	public void reset()
	{
		// isDrifting = wasDrifting = false;
		for( int i = 1; i < MaxEmitters; i++ )
		{
			emitters[i].setMaxParticleCount( 0 );
			emitters[i].reset();
		}
	}

	public void addEmitter( float x, float y )
	{
		ParticleEmitter next = emitters[emitterIdx++];
		if( emitterIdx == MaxEmitters ) emitterIdx = 0;

		next.setMaxParticleCount( MaxParticlesPerEmitter );
		next.setPosition( x, y );

//		next.setAdditive( false );
//		next.allowCompletion();
//		next.setContinuous( false );
//		next.duration = 1000;
//		next.durationTimer = 0;
//		next.start();
	}

	@Override
	public int getParticleCount()
	{
		int count = 0;
		for( int i = 1; i < MaxEmitters; i++ )
		{
			count += emitters[i].getActiveCount();
		}

		return count;
	}
}
