package com.bitfire.uracer.screen;

import com.badlogic.gdx.utils.LongMap;
import com.bitfire.uracer.screen.transitions.CrossFader;
import com.bitfire.uracer.screen.transitions.Fader;
import com.bitfire.uracer.screen.transitions.ScreenTransition;
import com.bitfire.uracer.utils.Hash;

public final class TransitionFactory {

	private static LongMap<ScreenTransition> transitions = new LongMap<ScreenTransition>();

	public enum TransitionType {
		None, CrossFader, Fader;

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
		switch( transitionType ) {
		case CrossFader:
			return new CrossFader();
		case Fader:
			return new Fader();
		default:
		case None:
			return null;
		}
	}

	public static void dispose() {
		for( ScreenTransition t : transitions.values() ) {
			t.dispose();
		}
	}
}
