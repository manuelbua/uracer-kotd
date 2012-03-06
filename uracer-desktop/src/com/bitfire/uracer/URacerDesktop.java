package com.bitfire.uracer;

import com.badlogic.gdx.backends.jogl.JoglApplication;
import com.badlogic.gdx.backends.jogl.JoglApplicationConfiguration;


public class URacerDesktop
{
	public static void main (String[] argv)
	{
		JoglApplicationConfiguration config = new JoglApplicationConfiguration();
		config.width = 1280; config.height = 752;
		config.samples = 0;
		config.depth = 0;
		config.vSyncEnabled = true;
		config.useGL20 = true;
		config.fullscreen = false;

		URacer uracer = new URacer();
		JoglApplication app = new JoglApplication(uracer, config );	// target

//		URacerDesktopFinalizer finalizr = new URacerDesktopFinalizer( (OpenALAudio)app.getAudio() );
//		uracer.setFinalizer( finalizr );
	}

}

//public class URacerDesktop
//{
//	public static void main( String[] argv )
//	{
//		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
//		config.title = "uRacer: The King Of The Drift";
//
//
//
//		config.width = 1280; config.height = 752;
////		config.width = 1280; config.height = 720;
////		config.width = 800; config.height = 480;
//
//		config.samples = 0;
//		config.depth = 0;
//		config.vSyncEnabled = true;
//		config.useCPUSynch = false;
//		config.useGL20 = true;
//		config.fullscreen = false;
//
//		URacer uracer = new URacer();
//		LwjglApplication app = new LwjglApplication(uracer, config);
//
//		URacerDesktopFinalizer finalizr = new URacerDesktopFinalizer( (OpenALAudio)app.getAudio() );
//		uracer.setFinalizer( finalizr );
//
//		int screenW = 1680; int screenH = 1050;
//		Display.setLocation( 1920 + (screenW-config.width)/2, (screenH-config.height)/2 );
//	}
//
//
//}