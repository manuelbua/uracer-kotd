package com.bitfire.uracer;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import org.lwjgl.opengl.Display;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.openal.OpenALAudio;

public final class URacerDesktop {

	private static boolean useRightScreen = false;

	private static boolean isInt( String value ) {
		if( value != null ) {
			int len = value.length();
			for( int i = 0; i < len; i++ ) {
				if( !Character.isDigit( value.charAt( i ) ) ) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	private static boolean parseConfig( String[] argv, LwjglApplicationConfiguration config ) {

		System.out.println( URacer.Name + " " + URacer.getVersionInformation() + "\nCopyright (c) 2012 Manuel Bua.\n" );

		// set to default
		config.title = URacer.Name;
		config.samples = 0;
		config.depth = 0;
		config.width = 800;
		config.height = 480;
		config.vSyncEnabled = true;
		config.useCPUSynch = false;
		config.useGL20 = true;
		config.fullscreen = false;
		config.resizable = false;

		// parse opts --
		int c;
		String arg;
		LongOpt[] opts = new LongOpt[ 6 ];
		opts[0] = new LongOpt( "help", LongOpt.NO_ARGUMENT, null, 'h' );
		opts[1] = new LongOpt( "resolution", LongOpt.REQUIRED_ARGUMENT, null, 'r' );
		opts[2] = new LongOpt( "no-vsync", LongOpt.NO_ARGUMENT, null, 'v' );
		opts[3] = new LongOpt( "cpusync", LongOpt.NO_ARGUMENT, null, 'C' );
		opts[4] = new LongOpt( "fullscreen", LongOpt.NO_ARGUMENT, null, 'f' );
		opts[4] = new LongOpt( "right-screen", LongOpt.NO_ARGUMENT, null, 't' );

		Getopt g = new Getopt( "URacer", argv, ":hr:vCft", opts );
		g.setOpterr( false );
		while( (c = g.getopt()) != -1 ) {
			arg = g.getOptarg();

			switch( c ) {
			case 0:
				System.out.println( "return 0, " + arg );
				break;
			case 1:
				System.out.println( "return 1, " + arg );
				break;
			case 2:
				System.out.println( "return 2, " + arg );
				break;
			case 'r':
				String[] res = arg.split( "x" );
				String msg = "";

				if( res.length == 2 && isInt( res[0] ) && isInt( res[1] ) ) {
					config.width = Integer.parseInt( res[0] );
					config.height = Integer.parseInt( res[1] );
				} else {
					if( arg.equals( "low" ) ) {
						config.width = 800;
						config.height = 480;
					} else if( arg.equals( "mid" ) ) {
						config.width = 1280;
						config.height = 800;
					} else if( arg.equals( "high" ) ) {
						config.width = 1920;
						config.height = 1080;
					} else {
						System.out.println( "Invalid resolution specified (" + arg + "), defaulting to " + (config.width + "x" + config.height) );
					}
				}

				break;
			case 'h':
				System.out.println( "Valid command-line options:\n" );
				System.out.println( "  -h, --help\t\tshows this help" );
				System.out.println( "  -r, --resolution=RES\tspecify the resolution to use: you can either specify" );
				System.out.println( "  \t\t\ta real resolution =, e.g. --resolution=800x600, or use " );
				System.out.println( "  \t\t\ta built-in shortcut (one of \"low\", \"mid\" or \"high\")." );
				System.out.println( "  \t\t\t(low=800x480, mid=1280x800, high=1920x1080)" );
				System.out.println( "  -v, --no-vsync\tdisable VSync" );
				System.out.println( "  -c, --cpusync\t\tenable CPU sync" );
				System.out.println( "  -f, --fullscreen\tenable fullscreen" );
				System.out.println( "  -t, --right-screen\treposition the game's window to the screen on the right,\n\t\t\tif available." );
				System.out.println( "" );
				return false;
			case 'v':
				config.vSyncEnabled = false;
				break;
			case 'C':
				config.useCPUSynch = true;
				break;
			case 'f':
				config.fullscreen = true;
				break;
			case 't':
				useRightScreen = true;
				break;
			case '?':
				System.out.println( "The specified parameter is not valid.\nTry --help for a list of valid parameters." );
				return false;
			case ':':
				System.out.println( "The specified argument is missing some values.\nTry --help for more information." );
				return false;
			default:
				System.out.println( "getopt() returned " + c + " (" + (char)c + ")\n" );
				return false;
			}
		}

		System.out.println( "Resolution set at " + (config.width + "x" + config.height) );
		System.out.println( "Vertical sync: " + (config.vSyncEnabled ? "On" : "Off") );
		System.out.println( "CPU sync: " + (config.useCPUSynch ? "On" : "Off") );
		System.out.println( "Fullscreen: " + (config.fullscreen ? "Yes" : "No") );

		return true;
	}

	public static void main( String[] argv ) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();

		if( !parseConfig( argv, config ) ) {
			return;
		}

		URacer uracer = new URacer();
		LwjglApplication app = new LwjglApplication( uracer, config );

		URacerDesktopFinalizer finalizr = new URacerDesktopFinalizer( (OpenALAudio)app.getAudio() );
		uracer.setFinalizer( finalizr );

		if( useRightScreen ) {
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice primary = env.getDefaultScreenDevice();
			GraphicsDevice[] devices = env.getScreenDevices();
			GraphicsDevice target = null;

			// search for the first target screen
			for( int i = 0; i < devices.length; i++ ) {
				boolean isPrimary = (primary == devices[i]);
				if( !isPrimary ) {
					target = devices[i];
					break;
				}
			}

			if( target != null ) {
				DisplayMode pmode = primary.getDisplayMode();
				DisplayMode tmode = target.getDisplayMode();

				Display.setLocation( pmode.getWidth() + (tmode.getWidth() - config.width) / 2, (tmode.getHeight() - config.height) / 2 );
			}
		}
	}

	private URacerDesktop() {
	}
}