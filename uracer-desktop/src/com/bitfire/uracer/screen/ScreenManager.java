package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;
import com.bitfire.uracer.screen.transitions.ScreenTransition;
import com.bitfire.uracer.screen.transitions.TransitionFactory;
import com.bitfire.uracer.screen.transitions.TransitionFactory.TransitionType;
import com.bitfire.uracer.screen.transitions.TransitionManager;

public final class ScreenManager {

	private ScalingStrategy strategy;
	private TransitionManager transMgr;
	private Screen current, next;
	private boolean quitPending, doSetScreenImmediate;

	public ScreenManager( ScalingStrategy scalingStrategy ) {
		transMgr = new TransitionManager( Config.isDesktop /* 32bits */, false, true );
		strategy = scalingStrategy;
		current = null;
		next = null;
		quitPending = false;
		doSetScreenImmediate = false;
	}

	public void dispose() {
		if( current != null ) {
			current.removed();
			current = null;
		}

		transMgr.dispose();
	}

	public boolean begin() {
		return !quitPending;
	}

	public void end() {
		if( (transMgr.isActive() && transMgr.isComplete()) || doSetScreenImmediate ) {
			doSetScreenImmediate = false;

			// transition finished, current is updated at the *very end*
			transMgr.removeTransition();
			current = next;
			next = null;

			// just switched to a null screen? Quit
			if( current == null ) {
				quitPending = true;
				Gdx.app.log( "GameLogic", "Quitting..." );
				Gdx.app.exit();
			}
		}
	}

	// FIXME, queue for buffered screen operations such as adding/removal
	/** Switch to the screen identified by the specified screen type,. */
	public void setScreen( ScreenType screen, TransitionType transitionType, long transitionDurationMs ) {
		if( transMgr.isActive() ) {
			// quit since already busy
			return;
		}

		doSetScreenImmediate = false;
		Screen newScreen = ScreenFactory.createScreen( screen, strategy );

		if( transitionType != TransitionType.None && transitionDurationMs > 0 ) {
			// create transition
			ScreenTransition transition = TransitionFactory.createTransition( transitionType );
			transition.setDuration( transitionDurationMs );

			next = newScreen;
			transMgr.start( current, next, transition );
		} else {
			next = newScreen;
			doSetScreenImmediate = true;
		}
	}

	public boolean quit() {
		return quitPending;
	}

	public void tick() {
		if( transMgr.isActive() ) {
			return;
		}

		if( current != null ) {
			current.tick();
		}
	}

	public void tickCompleted() {

		if( transMgr.isActive() ) {
			return;
		}

		if( current != null ) {
			current.tickCompleted();
		}
	}

	public void render( FrameBuffer dest ) {
		if( transMgr.isActive() ) {
			transMgr.update();
			transMgr.render();
		} else {
			if( current != null ) {
				current.render( dest );
			}
		}
	}

	public void debugRender() {
		if( transMgr.isActive() ) {
			return;
		}

		if( current != null ) {
			current.debugRender();
		}
	}

	public void pause() {
		if( quitPending ) {
			return;
		}

		if( transMgr.isActive() ) {
			transMgr.pause();
		} else {
			if( current != null ) {
				current.pause();
			}
		}
	}

	public void resume() {
		if( quitPending ) {
			return;
		}

		if( transMgr.isActive() ) {
			transMgr.resume();
		} else {
			if( current != null ) {
				current.resume();
			}
		}
	}
}
