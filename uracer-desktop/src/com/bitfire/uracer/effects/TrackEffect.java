package com.bitfire.uracer.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.effects.TrackEffects.Effects;

public abstract class TrackEffect {
	public Effects effectType;

	public TrackEffect( Effects what ) {
		this.effectType = what;
	}

	@Override
	public boolean equals( Object that ) {
		// self
		if( this == that ) return true;

		// not the same type
		if( !(that instanceof TrackEffect) ) return false;

		// safe cast
		TrackEffect other = (TrackEffect)that;

		// evaluate
		return this.effectType == other.effectType;
	}

	public abstract void tick();

	public abstract void reset();

	public abstract void dispose();

	public abstract void render( SpriteBatch batch );

	public abstract int getParticleCount();
}
