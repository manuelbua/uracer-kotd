package com.bitfire.uracer;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

//public class URacerDesktop
//{
//	public static void main (String[] argv) {
//		// (width!=height) && (width > height)
//
////		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 480, 320, true);
////		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 720, 480, true);
////		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 800, 480, true);
////		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 800, 800, true);
//		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 1280, 800, true);	// target
////		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 1280, 1024, true);
//
//		// higher resolutions than the target can't be supported without Track artifacts of
//		// some sort cropping out
////		new JoglApplication(new URacer(), "uRacer: The King Of The Drift", 1920, 1050, true);
//
//	}
//
//}

public class URacerDesktop
{
	public static void main( String[] argv )
	{
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "uRacer: The King Of The Drift";
		config.width = 1280;
		config.height = 800;
		config.samples = 0;
		config.depth = 0;
		config.vSyncEnabled = true;
		config.useCPUSynch = false;
		config.useGL20 = true;
		config.fullscreen = false;

		LwjglApplication app = new LwjglApplication(new URacer(), config);
		Display.setLocation( (1920-config.width)/2, (1080-config.height)/2 );
	}
}