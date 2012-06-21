package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;
import com.bitfire.uracer.screen.transitions.TransitionManager;

public final class ScreenManager {

	private ScalingStrategy strategy;
	private TransitionManager transMgr;
	private Screen current, next;
	private boolean quitPending;

	public ScreenManager( ScalingStrategy scalingStrategy ) {
		transMgr = new TransitionManager( Config.isDesktop /* 32bits */, false );
		strategy = scalingStrategy;
		current = null;
		next = null;
		quitPending = false;
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
		if( transMgr.isActive() && transMgr.hasFinished() ) {
			// transition finished
			transMgr.removeTransition();
			current = next;
			next = null;

			// if the current screen is null, then quit
			if( current == null ) {
				quitPending = true;
				Gdx.app.log( "GameLogic", "Quitting..." );
				Gdx.app.exit();
			}
		}
	}

	public void setScreen( ScreenType screen ) {
		if( transMgr.isActive() ) {
			// quit since already busy
			return;
		}

		next = ScreenFactory.createScreen( screen, strategy );
		transMgr.start( current, next );
	}

	public boolean quit() {
		return quitPending;
	}

	public void tick() {
		if( transMgr.isActive() ) {
			return;
		}

		current.tick();
	}

	public void tickCompleted() {

		if( transMgr.isActive() ) {
			return;
		}

		current.tickCompleted();
	}

	public void render( FrameBuffer dest ) {
		if( transMgr.isActive() ) {
			transMgr.update();
			transMgr.render();
		} else {
			current.render( dest );
		}
	}

	public void debugUpdate() {
		if( transMgr.isActive() ) {
			return;
		}

		current.debugUpdate();
	}

	public void pause() {
		if( quitPending ) {
			return;
		}

		if( transMgr.isActive() ) {
			transMgr.pause();
		} else {
			current.pause();
		}
	}

	public void resume() {
		if( quitPending ) {
			return;
		}

		if( transMgr.isActive() ) {
			transMgr.resume();
		} else {
			current.resume();
		}
	}
}
