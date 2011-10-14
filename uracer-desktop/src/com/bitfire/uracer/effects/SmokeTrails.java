package com.bitfire.uracer.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DriftInfo;

public class SmokeTrails extends TrackEffect
{
	private ParticleEffect effect;
	private ParticleEmitter baseEmitter;
	private ParticleEmitter[] emitters;
	private int emitterIdx;

	private final int MaxEmitters = 100;
	private final int MaxParticlesPerEmitterPerSec = 5;

	private boolean isDrifting, wasDrifting, isOn;
	private DriftInfo drift;
	private Car player;

	public SmokeTrails( Car player )
	{
		super( Effects.SmokeTrails );

		effect = new ParticleEffect();
		effect.load( Gdx.files.internal( "data/partfx/smoke.p" ), Gdx.files.internal( "data/partfx" ) );

		emitters = new ParticleEmitter[ MaxEmitters ];
		baseEmitter = effect.getEmitters().get( 0 );

		emitters[0] = baseEmitter;
		for( int i = 1; i < MaxEmitters; i++ )
		{
			emitters[i] = new ParticleEmitter( baseEmitter );
			effect.getEmitters().add( emitters[i] );
		}

		for( int i = 0; i < MaxEmitters; i++ )
		{
			emitters[i].setAdditive( false );
			emitters[i].getLife().setHigh( 850 );
//			emitters[i].setAttached( true );
			emitters[i].reset();
			off(i);
		}


		emitterIdx = 0;
		this.player = player;
		isDrifting = wasDrifting = isOn = false;
		drift = DriftInfo.get();
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

//		effect.setPosition( player.state().position.x, player.state().position.y );
//		effect.update( Physics.dt );

		// wasDrifting = isDrifting;

//		System.out.println( "smoke particles=" + getParticleCount() );
	}


	@Override
	public void render( SpriteBatch batch )
	{
//		effect.setPosition( player.state().position.x, player.state().position.y );
		effect.draw( batch, Gdx.graphics.getDeltaTime() );

//		if( isOn && effect.isComplete() )
//		{
//			effect.start();
//		}
	}

	private void off(int emitterIndex)
	{
		emitters[emitterIndex].getEmission().setHigh( 0 );
		emitters[emitterIndex].setMaxParticleCount( 0 );
		emitters[emitterIndex].allowCompletion();
		isOn = false;
	}

	private void on(int emitterIndex)
	{
		emitters[emitterIndex].getEmission().setHigh( MaxParticlesPerEmitterPerSec );
		emitters[emitterIndex].setMaxParticleCount( MaxParticlesPerEmitterPerSec );
		emitters[emitterIndex].start();
		isOn = true;
	}

	@Override
	public void reset()
	{
		isDrifting = wasDrifting = isOn = false;
		for( int i = 0; i < MaxEmitters; i++ )
		{
			off( i );
		}
	}

	private Vector2 tmp = new Vector2();
	public void addEmitter( float x, float y )
	{
		ParticleEmitter next = emitters[emitterIdx];

		tmp.set(x,y);
//		tmp.set( Director.positionFor( tmp ) );	// y-flip pixel-based word position
		on( emitterIdx );
		next.setPosition( tmp.x, tmp.y );

		next.setContinuous(false);
		next.duration = 850;
		next.durationTimer = 0;

		emitterIdx++;
		if( emitterIdx == MaxEmitters ) emitterIdx = 0;
	}

	@Override
	public int getParticleCount()
	{
//		return effect.getEmitters().get( 0 ).getActiveCount();

		int count = 0;
		for( int i = 0; i < MaxEmitters; i++ )
		{
			count += emitters[i].getActiveCount();
		}

		return count;
	}
}
