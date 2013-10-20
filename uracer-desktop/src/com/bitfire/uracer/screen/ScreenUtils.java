
package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/** Implements useful Screen objects related utilities. */
public final class ScreenUtils {

	public static Screen currentScreen;

	/** Render the specified screen to the specified buffer. */
	public static void copyScreen (Screen screen, FrameBuffer buffer, Color clearColor, float clearDepth, boolean useDepth) {
		if (screen != null) {
			clear(buffer, clearColor, clearDepth, useDepth);

			// ensures default active texture is active
			Gdx.gl20.glActiveTexture(GL20.GL_TEXTURE0);

			screen.render(buffer);
		}
	}

	/** Render the specified screen to the specified buffer. */
	public static void copyScreen (Screen screen, FrameBuffer buffer) {
		copyScreen(screen, buffer, Color.BLACK, 1f, false);
	}

	/** Clear the specified buffer. */
	public static void clear (FrameBuffer buffer, Color clearColor, float clearDepth, boolean useDepth) {
		Gdx.gl20.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);

		buffer.begin();
		{
			if (useDepth) {
				Gdx.gl20.glClearDepthf(clearDepth);
				Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			} else {
				Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
			}
		}
		buffer.end();
	}

	/** Clear the specified buffer. */
	public static void clear (FrameBuffer buffer, Color clearColor) {
		clear(buffer, clearColor, 1f, false);
	}

	private ScreenUtils () {
	}
}
