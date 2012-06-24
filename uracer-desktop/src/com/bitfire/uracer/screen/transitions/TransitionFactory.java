package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.utils.Hash;

public final class TransitionFactory {

	private static LongMap<ScreenTransition> transitions = new LongMap<ScreenTransition>();

	public enum TransitionType {
		None, Fader;

		public long hash;
		private TransitionType() {
			hash = Hash.APHash( this.name() );
		}
	}

	private TransitionFactory() {
	}

	public static ScreenTransition getTransition( TransitionType transitionType ) {
		ScreenTransition transition = null;

		if( transitions.containsKey( transitionType.hash ) ) {
			transition = transitions.get( transitionType.hash );
			transition.reset();
		} else {
			transition = createTransition( transitionType );
			transitions.put( transitionType.hash, transition );
		}

		return transition;
	}

	private static ScreenTransition createTransition( TransitionType transitionType ) {
		ScreenTransition transition = null;

		switch( transitionType ) {
		case Fader:
			transition = new Fader();
			break;
		default:
		case None:
			transition = null;
			break;
		}

		return transition;
	}

	public static void dispose() {
		for( ScreenTransition t : transitions.values() ) {
			t.dispose();
		}
	}
}
