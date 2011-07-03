package com.bitfire.uracer;

import com.badlogic.gdx.backends.jogl.JoglApplication;

public class URacerDesktop
{
	public static void main (String[] argv) {
//		new JoglApplication(new TestTilemap(), "test-tilemap", 320, 480, true);
		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 480, 320, true);
//		new JoglApplication(new TestTilemap(), "test-tilemap", 960, 640, true);
//		new JoglApplication(new TestTilemap(), "test-tilemap", 1280, 800, true);
	}

}
