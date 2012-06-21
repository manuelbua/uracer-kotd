package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.screen.Screen;

public final class TransitionManager {

	boolean paused, usedepth;
	Format fbFormat;
	FrameBuffer fbFrom, fbTo;
	ScreenTransition transition;

	public TransitionManager( boolean use32Bits, boolean useAlphaChannel, boolean useDepth ) {
		transition = null;
		paused = false;
		fbFormat = Format.RGB565;
		usedepth = useDepth;

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

		fbFrom = new FrameBuffer( fbFormat, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), useDepth );
		fbTo = new FrameBuffer( fbFormat, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), useDepth );
	}

	public void dispose() {
		fbFrom.dispose();
		fbTo.dispose();
	}

	private void initFrameBuffer( FrameBuffer buffer, Screen source ) {
		Gdx.gl20.glClearColor( 0, 0, 0, 0 );
		buffer.begin();
		{
			if( usedepth ) {
				Gdx.gl20.glClearDepthf( 1 );
				Gdx.gl20.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );
			} else {
				Gdx.gl20.glClear( GL20.GL_COLOR_BUFFER_BIT );
			}
		}
		buffer.end();

		if( source != null ) {
			source.tick();
			source.tickCompleted();
			source.render( buffer );

			if( Config.Debug.RenderDebugDrawsInTransitions ) {
				buffer.begin();
				source.debugRender();
				buffer.end();
			}
		}
	}

	/** Starts the specified transition (transfer ownerships) */
	public void start( Screen curr, Screen next, ScreenTransition transition ) {
		removeTransition();
		this.transition = transition;

		// build source textures
		initFrameBuffer( fbFrom, curr );
		initFrameBuffer( fbTo, next );

		// enable depth writing if its the case
		Gdx.gl20.glDepthMask( usedepth );
		this.transition.init( fbFrom, fbTo );
	}

	public boolean isActive() {
		return (transition != null);
	}

	public boolean isComplete() {
		return transition.isComplete();
	}

	public void removeTransition() {
		if( transition != null ) {
			transition.dispose();
			transition = null;
		}
	}

	public void pause() {
		if( paused ) {
			return;
		}

		paused = true;
		if( transition != null ) {
			transition.pause();
		}
	}

	public void resume() {
		if( !paused ) {
			return;
		}

		paused = false;
		if( transition != null ) {
			transition.resume();
		}
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
		if( paused ) {
			return;
		}

		if( transition != null ) {
			transition.render();
		}
	}
}