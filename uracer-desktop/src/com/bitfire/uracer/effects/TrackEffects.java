package com.bitfire.uracer.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.GameLogic;

public class TrackEffects {
	public enum Effects {
		CarSkidMarks( 1 ), SmokeTrails( 2 );

		public final long id;

		private Effects( int id ) {
			this.id = id;
		}

		public long asLong() {
			return id;
		}

		public int asInt() {
			return (int)id;
		}
	}

	private static Car playerCar;

	private static LongMap<TrackEffect> effects;

	public static void init( GameLogic logic ) {
		playerCar = logic.getGame().getLevel().getPlayer().car;
		effects = new LongMap<TrackEffect>();

		TrackEffects.add( Effects.CarSkidMarks );
		// TrackEffects.add( Effects.SmokeTrails );
	}

	/** manage effects */

	private static void add( Effects what ) {
		switch( what ) {
		case CarSkidMarks:
			add( new CarSkidMarks( playerCar ) );
			break;
		case SmokeTrails:
			add( new SmokeTrails( playerCar ) );
			break;
		}
	}

	private static long add( TrackEffect effect ) {
		long result = 0;
		if( effect != null ) {
			result = effect.effectType.id;
			effects.put( effect.effectType.id, effect );
		}

		return result;
	}

	private static boolean remove( Effects what ) {
		TrackEffect removed = effects.remove( what.id );
		if( removed != null ) {
			removed.dispose();
			return true;
		}

		return false;
	}

	public static TrackEffect get( Effects what ) {
		return effects.get( what.id );
	}

	/** life */

	public static void tick() {
		for( TrackEffect effect : effects.values() )
			effect.tick();
	}

	public static void reset() {
		for( TrackEffect effect : effects.values() )
			effect.reset();
	}

	public static void dispose() {
		remove( Effects.CarSkidMarks );
		remove( Effects.SmokeTrails );
	}

	public static void render( SpriteBatch batch ) {
		TrackEffect effect;

		effect = effects.get( TrackEffects.Effects.CarSkidMarks.id );
		if( Config.Graphics.hasEffect( TrackEffects.Effects.CarSkidMarks.id ) && effect != null ) effect.render( batch );

		effect = effects.get( TrackEffects.Effects.SmokeTrails.id );
		if( Config.Graphics.hasEffect( TrackEffects.Effects.SmokeTrails.id ) && effect != null ) effect.render( batch );
	}

	/** expose effects TODO find a more sensible way without incurring in overhead */

	public static void renderEffect( Effects what, SpriteBatch batch ) {
		effects.get( what.id ).render( batch );
	}

	public static int getParticleCount( Effects what ) {
		TrackEffect effect = effects.get( what.id );
		if( effect == null ) return 0;
		return effect.getParticleCount();
	}
}
