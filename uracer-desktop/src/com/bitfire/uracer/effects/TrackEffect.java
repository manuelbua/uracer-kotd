package com.bitfire.uracer.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class TrackEffect {
	public enum Type {
		CarSkidMarks( 1 ), SmokeTrails( 2 );	// FIXME, effects are destined to be drawn in this precise order in the same queue
		public final int id;

		private Type( int id ) {
			this.id = id;
		}
	}

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

