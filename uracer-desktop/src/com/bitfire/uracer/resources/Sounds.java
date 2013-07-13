
package com.bitfire.uracer.resources;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public final class Sounds {

	public static Sound carDrift;
	public static Sound carEngine;
	public static Sound[] carImpacts = new Sound[5];
	public static Sound[] musTensive = new Sound[7];

	public static void init () {
		carDrift = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/drift-loop.ogg", FileType.Internal));
		carEngine = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/engine.ogg", FileType.Internal));

		carImpacts[0] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-2.ogg", FileType.Internal)); // low1
		carImpacts[1] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-3.ogg", FileType.Internal)); // low2
		carImpacts[2] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-1.ogg", FileType.Internal)); // mid1
		carImpacts[3] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-4.ogg", FileType.Internal)); // mid2
		carImpacts[4] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-5.ogg", FileType.Internal)); // high

		musTensive[0] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/prologue-1.ogg", FileType.Internal));
		musTensive[1] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/prologue-2.ogg", FileType.Internal));
		musTensive[2] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/prologue-3.ogg", FileType.Internal));
		musTensive[3] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/prologue-4.ogg", FileType.Internal));
		musTensive[4] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/inciso-1.ogg", FileType.Internal));
		musTensive[5] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/inciso-2.ogg", FileType.Internal));
		musTensive[6] = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/tensive/inciso-3.ogg", FileType.Internal));
	}

	public static void dispose () {
		carDrift.dispose();
		carEngine.dispose();

		for (int i = 0; i < carImpacts.length; i++) {
			carImpacts[i].dispose();
		}

		for (int i = 0; i < musTensive.length; i++) {
			musTensive[i].dispose();
		}
	}

	private Sounds () {
	}
}
