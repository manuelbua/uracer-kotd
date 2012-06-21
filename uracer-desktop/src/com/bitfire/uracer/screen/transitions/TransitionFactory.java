package com.bitfire.uracer.screen.transitions;

public final class TransitionFactory {

	public enum TransitionType {
		None, Fader
	}

	public static ScreenTransition createTransition( TransitionType transitionType ) {
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
}
