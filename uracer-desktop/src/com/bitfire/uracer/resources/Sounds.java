package com.bitfire.uracer.resources;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public final class Sounds {

	public static Sound carDrift;
	public static Sound carEngine;
	public static Sound[] carImpacts = new Sound[ 5 ];

	public static void init() {
		carDrift = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/drift-loop.ogg", FileType.Internal ) );
		carEngine = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/engine.ogg", FileType.Internal ) );
		carImpacts[0] = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-2.ogg", FileType.Internal ) ); // low1
		carImpacts[1] = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-3.ogg", FileType.Internal ) ); // low2
		carImpacts[2] = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-1.ogg", FileType.Internal ) ); // mid1
		carImpacts[3] = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-4.ogg", FileType.Internal ) ); // mid2
		carImpacts[4] = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-5.ogg", FileType.Internal ) ); // high
	}

	public static void dispose() {
		carDrift.dispose();
		carEngine.dispose();
		for( int i = 0; i < carImpacts.length; i++ ) {
			carImpacts[i].dispose();
		}
	}

	private Sounds() {
	}
}
