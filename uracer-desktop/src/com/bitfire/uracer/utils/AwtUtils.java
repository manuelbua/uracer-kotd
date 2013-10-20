
package com.bitfire.uracer.utils;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class AwtUtils {

	public static DisplayMode getNativeDisplayMode () {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice primary = env.getDefaultScreenDevice();
		if (primary != null) {
			return primary.getDisplayMode();
		}

		return null;
	}

	public static int getCenteredXOnDisplay (int width) {
		DisplayMode mode = getNativeDisplayMode();
		return (mode.getWidth() - width) / 2;
	}

	public static int getCenteredYOnDisplay (int height) {
		DisplayMode mode = getNativeDisplayMode();
		return (mode.getHeight() - height) / 2;
	}
}
