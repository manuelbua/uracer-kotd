
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent.Order;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.SoundManager;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class PlayerTensiveMusic extends SoundEffect {
	public static final int NumTracks = 7;
	private static final float MinVolume = 0.4f;

	private Sound[] music = new Sound[NumTracks]; // prologue [0,3], inciso [4,6]
	private long[] mid = new long[NumTracks];
	private boolean[] started = new boolean[NumTracks];

	private boolean paused;
	private float[] lastVolume = new float[NumTracks];
	private TrackProgressData progressData;
	private LapManager lapManager;
	private InterpolatedFloat[] volTrack = new InterpolatedFloat[NumTracks];
	private float[] volOut = new float[NumTracks];
	private int musicIndex, musicIndexLimit;

	public PlayerTensiveMusic (TrackProgressData progressData, LapManager lapManager) {
		this.progressData = progressData;
		this.lapManager = lapManager;

		paused = false;
		musicIndex = 0;
		for (int i = 0; i < NumTracks; i++) {
			started[i] = false;
			lastVolume[i] = 0;
			mid[i] = -1;
			music[i] = Sounds.musTensive[i];
			volTrack[i] = new InterpolatedFloat();
		}
	}

	@Override
	public void dispose () {
		detach();
		stop();
	}

	private PlayerLapCompletionMonitorEvent.Listener playerCompletionListener = new PlayerLapCompletionMonitorEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			start();
		}
	};

	private void attach () {
		GameEvents.lapCompletion.addListener(playerCompletionListener, PlayerLapCompletionMonitorEvent.Type.onWarmUpStarted);
	}

	private void detach () {
		GameEvents.lapCompletion.removeListener(playerCompletionListener, PlayerLapCompletionMonitorEvent.Type.onWarmUpStarted);
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		if (hasPlayer) {
			attach();
		} else {
			detach();
			stop();
		}
	}

	private void start () {
		for (int i = 0; i < NumTracks; i++) {
			start(i);
		}
	}

	private void start (int track) {
		if (started[track]) {
			return;
		}

		started[track] = true;
		mid[track] = loop(music[track], 0);
		music[track].setVolume(mid[track], 0);
	}

	@Override
	public void stop () {
		for (int i = 0; i < NumTracks; i++) {
			stop(i);
		}
	}

	public void stop (int track) {
		if (!started[track]) {
			return;
		}

		started[track] = false;
		if (mid[track] > -1) {
			music[track].stop(mid[track]);
		}
	}

	@Override
	public void gamePause () {
		// paused = true;
	}

	@Override
	public void gameResume () {
		paused = false;
	}

	@Override
	public void gameReset () {
		gameRestart();
	}

	@Override
	public void gameRestart () {
		// stop();
		for (int i = 0; i < NumTracks; i++) {
			volTrack[i].reset(false);
		}
	}

	private void setVolume (int track, float volume) {
		float v = MathUtils.clamp(volume, 0, 1);
		lastVolume[track] = v;
		music[track].setVolume(mid[track], v);
	}

	public float[] getVolumes () {
		return volOut;
	}

	public int getMusicIndex () {
		return musicIndex;
	}

	public int getCurrentMusicIndexLimit () {
		return musicIndexLimit;
	}

	@Override
	public void tick () {
		float scalemt = 40;
		float rangeVol = 1 - MinVolume;
		float tgt_vol = 0;

		// limit to number of actual replays
		musicIndexLimit = MathUtils.clamp(lapManager.getReplaysCount(), 0, NumTracks - 2);

		if (hasPlayer) {

			// assumes index 0 (player in disadvantage)
			musicIndex = 0;

			// default interpolation speed
			float alpha = 0.05f;

			if (!progressData.isWarmUp && progressData.hasTarget && !progressData.targetArrived) {

				// slow down interpolation speed, but keep it up anyway when slowing down time
				alpha = 0.02f / URacer.timeMultiplier;

				float v = progressData.playerDistance.get() - progressData.targetDistance.get();
				float to_target = AMath.fixup(MathUtils.clamp(v / scalemt, -1, 1));
				tgt_vol = 1 - MathUtils.clamp(-to_target, 0, 1);

				if (to_target > 0.5f && lapManager.getReplaysCount() >= NumTracks) {
					// player ahead by 20mt
					musicIndex = NumTracks - 1;
					musicIndexLimit = NumTracks - 1;
				} else if (to_target > 0) {
					// player is heading the race
					musicIndex = musicIndexLimit;
				} else {
					float fidx = tgt_vol * musicIndexLimit;
					musicIndex = progressData.isWarmUp ? 0 : (int)fidx;
				}

				// Gdx.app.log("PlayerTensiveMusic", "to_target=" + to_target);
			}

			// update all volume accumulators

			// tgt_vol = 1;
			// musicIndex = 5;
			float step = 1f / (float)(NumTracks - 1);

			for (int i = 0; i <= NumTracks - 1; i++) {
				if (i == musicIndex && i <= musicIndexLimit) {
					float v = MathUtils.clamp(step * musicIndex, MinVolume, 1);
					v *= SoundManager.MusicVolumeMul;
					volTrack[i].set(v, alpha);
				} else {
					volTrack[i].set(0, alpha);
				}

				// interpolate and get
				volOut[i] = volTrack[i].get();

				setVolume(i, volOut[i]);
			}
		}
	}
}
