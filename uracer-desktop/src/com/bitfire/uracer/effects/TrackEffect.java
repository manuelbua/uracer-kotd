package com.bitfire.uracer.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.effects.TrackEffects.Type;

public abstract class TrackEffect {
	public Type type;

	public TrackEffect( Type what ) {
		this.type = what;
	}

	public abstract void onTick();

	public abstract void reset();

	public abstract void dispose();

	public abstract void render( SpriteBatch batch );

	public abstract int getParticleCount();
}
