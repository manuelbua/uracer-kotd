
package com.bitfire.uracer.resources;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.configuration.Storage;

public final class Sounds {

	public static Sound carDrift;
	public static Sound[] carEngine_f40 = new Sound[7];
	public static Sound[] carEngine_a1 = new Sound[7];
	public static Sound[] carEngine_f1 = new Sound[7];
	public static Sound[] carEngine_msc = new Sound[7];

	public static Sound[] carImpacts = new Sound[8];
	public static Sound[] musTensive = new Sound[7];

	public static Sound menuClick;
	public static Sound menuRollover;
	public static Sound menuSwitch;

	public static void init () {
		carDrift = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-tires/drift-loop.ogg", FileType.Internal));

		//@off

		carImpacts[0] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-2.ogg", FileType.Internal)); // low1
		carImpacts[1] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-3.ogg", FileType.Internal)); // low2
		carImpacts[2] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-7.ogg", FileType.Internal)); // low3
		carImpacts[3] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-8.ogg", FileType.Internal)); // low4
		carImpacts[4] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-1.ogg", FileType.Internal)); // mid1
		carImpacts[5] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-4.ogg", FileType.Internal)); // mid2
		carImpacts[6] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-6.ogg", FileType.Internal)); // mid3
		carImpacts[7] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-crashes/impact-5.ogg", FileType.Internal)); // high

		musTensive[0] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/prologue-1.ogg", FileType.Internal));
		musTensive[1] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/prologue-2.ogg", FileType.Internal));
		musTensive[2] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/prologue-3.ogg", FileType.Internal));
		musTensive[3] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/prologue-4.ogg", FileType.Internal));
		musTensive[4] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/inciso-1.ogg", FileType.Internal));
		musTensive[5] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/inciso-2.ogg", FileType.Internal));
		musTensive[6] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "tensive/inciso-3.ogg", FileType.Internal));

		// f40
		carEngine_f40[0] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/idle.wav", FileType.Internal));
		carEngine_f40[1] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/onlow.wav", FileType.Internal));
		carEngine_f40[2] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/onmid.wav", FileType.Internal));
		carEngine_f40[3] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/onhigh.wav", FileType.Internal));
		carEngine_f40[4] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/offlow.wav", FileType.Internal));
		carEngine_f40[5] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/offmid.wav", FileType.Internal));
		carEngine_f40[6] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f40/offhigh.wav", FileType.Internal));

		// a1 Boxster
		carEngine_a1[0] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/idle_ext.wav", FileType.Internal));
		carEngine_a1[1] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/on_2000_ext.wav", FileType.Internal));
		carEngine_a1[2] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/on_3000_ext.wav", FileType.Internal));
		carEngine_a1[3] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/on_6000_ext.wav", FileType.Internal));
		carEngine_a1[4] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/off_2000_ext.wav", FileType.Internal));
		carEngine_a1[5] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/off_3000_ext.wav", FileType.Internal));
		carEngine_a1[6] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/a1/off_6000_ext.wav", FileType.Internal));

		// f1 RF1V8
		carEngine_f1[0] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/idle.wav", FileType.Internal));
		carEngine_f1[1] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/onlow.wav", FileType.Internal));
		carEngine_f1[2] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/onmid.wav", FileType.Internal));
		carEngine_f1[3] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/onhigh.wav", FileType.Internal));
		carEngine_f1[4] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/offlow.wav", FileType.Internal));
		carEngine_f1[5] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/offmid.wav", FileType.Internal));
		carEngine_f1[6] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/f1/offhigh.wav", FileType.Internal));

		// muscle
		carEngine_msc[0] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/idle.wav", FileType.Internal));
		carEngine_msc[1] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/onlow.wav", FileType.Internal));
		carEngine_msc[2] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/onmid.wav", FileType.Internal));
		carEngine_msc[3] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/onhigh.wav", FileType.Internal));
		carEngine_msc[4] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/offlow.wav", FileType.Internal));
		carEngine_msc[5] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/offmid.wav", FileType.Internal));
		carEngine_msc[6] = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "car-engine/muscle/offhigh.wav", FileType.Internal));

		// menu sfx
		menuClick = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "menu-sfx/click1.ogg", FileType.Internal));
		menuRollover = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "menu-sfx/rollover2.ogg", FileType.Internal));
		menuSwitch = Gdx.audio.newSound(Gdx.files.getFileHandle(Storage.Audio + "menu-sfx/switch2.ogg", FileType.Internal));
		
		//@on
	}

	public static void dispose () {
		carDrift.dispose();

		for (int i = 0; i < carImpacts.length; i++) {
			carImpacts[i].dispose();
		}

		for (int i = 0; i < musTensive.length; i++) {
			musTensive[i].dispose();
		}

		for (int i = 0; i < 7; i++) {
			carEngine_f40[i].dispose();
			carEngine_a1[i].dispose();
			carEngine_f1[i].dispose();
			carEngine_msc[i].dispose();
		}

		menuClick.dispose();
		menuRollover.dispose();
		menuSwitch.dispose();
	}

	private Sounds () {
	}
}
