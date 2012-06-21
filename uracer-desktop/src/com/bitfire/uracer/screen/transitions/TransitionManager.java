package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.screen.Screen;

public final class TransitionManager {

	boolean paused;
	Format fbFormat;
	FrameBuffer fbFrom, fbTo;
	ScreenTransition transition;

	public TransitionManager( boolean use32Bits, boolean useAlphaChannel ) {
		transition = null;
		paused = false;
		fbFormat = Format.RGB565;

		if( use32Bits ) {
			if( useAlphaChannel ) {
				fbFormat = Format.RGBA8888;
			} else {
				fbFormat = Format.RGB888;
			}
		} else {
			if( useAlphaChannel ) {
				fbFormat = Format.RGBA4444;
			} else {
				fbFormat = Format.RGB565;
			}
		}

		fbFrom = new FrameBuffer( fbFormat, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false );
		fbTo = new FrameBuffer( fbFormat, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false );
	}

	public void dispose() {
		fbFrom.dispose();
		fbTo.dispose();
	}

	private void initFrameBuffer( FrameBuffer buffer, Screen source ) {
		if( source != null ) {
			source.tick();
			source.tickCompleted();
			source.render( buffer );
		} else {
			buffer.begin();
			Gdx.gl20.glClearDepthf( 1 );
			Gdx.gl20.glClearColor( 0, 0, 0, 0 );
			Gdx.gl20.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );
			buffer.end();
		}
	}

	public void start( Screen curr, Screen next ) {
		removeTransition();
		transition = new Fader( 1 );

		initFrameBuffer( fbFrom, curr );
		initFrameBuffer( fbTo, next );

		Gdx.gl20.glDepthMask( false );
		transition.init( fbFrom, fbTo );
	}

	public boolean isActive() {
		return (transition != null);
	}

	public boolean hasFinished() {
		return transition.hasFinished();
	}

	public void removeTransition() {
		if( transition != null ) {
			transition.dispose();
			transition = null;
		}
	}

	public void pause() {
		paused = true;
	}

	public void resume() {
		paused = false;
	}

	public void update() {
		if( paused ) {
			return;
		}

		if( transition != null ) {
			transition.update();
		}
	}

	public void render() {
		Gdx.gl20.glClearDepthf( 1 );
		Gdx.gl20.glClearColor( 0, 0, 0, 0 );
		Gdx.gl20.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );

		if( paused ) {
			return;
		}

		if( transition != null ) {
			transition.render();
		}
	}
}