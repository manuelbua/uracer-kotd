package com.bitfire.uracer.game.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public abstract class TrackEffect implements Disposable {
	/** Defines the type of special effect, it also describer their rendering order */
	public enum Type {
		CarSkidMarks( 1 ), SmokeTrails( 2 );
		public final int id;

		private Type( int id ) {
			this.id = id;
		}
	}

	public Type type;

	public TrackEffect( Type what ) {
		this.type = what;
	}

	public abstract void tick();

	public abstract void reset();

	public abstract void render( SpriteBatch batch );

	public abstract int getParticleCount();
}
