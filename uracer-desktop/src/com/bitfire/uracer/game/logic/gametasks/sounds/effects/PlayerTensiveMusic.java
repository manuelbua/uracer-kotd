
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
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.InterpolatedFloat;

public final class PlayerTensiveMusic extends SoundEffect {
	public static final int NumTracks = 7;
	public static final float MinVolume = 0.25f;

	public static final float ScaleMt = 20f * (NumTracks - 2);
	public static final float AheadByMt = 10f;
	public static final float InvScaleMt = 1f / ScaleMt;

	private Sound[] music = new Sound[NumTracks]; // prologue [0,3], inciso [4,6]
	private long[] mid = new long[NumTracks];
	private boolean[] started = new boolean[NumTracks];

	private float[] lastVolume = new float[NumTracks];
	private TrackProgressData progressData;
	private InterpolatedFloat[] volTrack = new InterpolatedFloat[NumTracks];
	private float[] volOut = new float[NumTracks];
	private int musicIndex, musicIndexLimit;
	private float fMusicIndex;
	private float[] trackVolumes = new float[NumTracks];

	public PlayerTensiveMusic (TrackProgressData progressData) {
		this.progressData = progressData;

		musicIndex = 0;
		fMusicIndex = 0;
		for (int i = 0; i < NumTracks; i++) {
			started[i] = false;
			lastVolume[i] = 0;
			mid[i] = -1;
			music[i] = Sounds.musTensive[i];
			volTrack[i] = new InterpolatedFloat();
			computeTrackVolumes();
		}
	}

	private void computeTrackVolumes () {
		float step = (1f - MinVolume) / (float)(NumTracks - 1);
		for (int i = 0; i < NumTracks; i++) {
			float this_step = (step * i) + MinVolume;
			trackVolumes[i] = this_step;
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
		super.gamePause();
		for (int i = 0; i < NumTracks; i++) {
			volTrack[i].reset(false);
			setVolume(i, volTrack[i].get());
		}
	}

	@Override
	public void gameReset () {
		gameRestart();
	}

	@Override
	public void gameRestart () {
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
		if (isPaused) return;

		float tgt_vol = 0;
		// boolean isAheadByMeters = false;

		// limit to number of actual replays
		musicIndexLimit = NumTracks - 2;// MathUtils.clamp(lapManager.getReplaysCount(), 0, NumTracks - 2);
		// musicIndexLimit = NumTracks - 2;

		if (hasPlayer) {

			// assumes index 0 (player in disadvantage)
			musicIndex = 0;
			fMusicIndex = 0;

			// default interpolation speed
			float alpha_inc = 0.05f;
			float alpha_inc_next = 0.025f;
			float alpha_dec = 0.02f;

			if (!progressData.isWarmUp && progressData.hasTarget && !progressData.targetArrived) {

				// slow down interpolation speed, but bring it back when slowing down time
				alpha_inc = 0.02f / URacer.timeMultiplier;

				float v = progressData.playerDistance.get() - progressData.targetDistance.get();
				v = MathUtils.clamp(v, -ScaleMt, ScaleMt);
				v *= InvScaleMt; // normalized range
				float to_target = AMath.fixup(v);

				if (to_target >= (AheadByMt * InvScaleMt) && musicIndexLimit == NumTracks - 2) {
					// player ahead by AheadByMt meters, can play very latest track
					musicIndex = NumTracks - 1;
					fMusicIndex = musicIndex;
					musicIndexLimit = musicIndex;
				} else if (to_target > 0) {
					// player is heading the race
					musicIndex = musicIndexLimit;
					fMusicIndex = musicIndex;
				} else {
					tgt_vol = 1 - MathUtils.clamp(-to_target, 0, 1);
					fMusicIndex = tgt_vol * musicIndexLimit;
					musicIndex = (int)fMusicIndex;
					// Gdx.app.log("PlayerTensiveMusic", "to_target=" + to_target + ", tgt_vol=" + tgt_vol + ", midx=" + musicIndex);
					// Gdx.app.log("PlayerTensiveMusic", "to_target=" + to_target + ", tgt_vol=" + tgt_vol + ", midx=" + musicIndex);
					// Gdx.app.log("PlayerTensiveMusic", "fmusidx=" + fMusicIndex + ", limit=" + musicIndexLimit);
				}
			}

			// String targetString = "";
			// if (progressData.hasTarget) {
			// targetString = ", td=" + progressData.targetDistance.get();
			// }
			//
			// Gdx.app.log("PlayerTensiveMusic", "mi=" + musicIndex + ", limit=" + musicIndexLimit + ", pd="
			// + progressData.playerDistance.get() + targetString);

			// update all volume accumulators
			// float step = (1f - MinVolume) / (float)(NumTracks - 1);
			for (int i = 0; i <= NumTracks - 1; i++) {
				if (i == musicIndex && i <= musicIndexLimit) {
					float decimal = AMath.fixup(fMusicIndex - musicIndex);

					float vol_this = MathUtils.clamp((1 - decimal) * trackVolumes[i], MinVolume, 1);
					volTrack[i].set(vol_this, alpha_inc);

					int next = i + 1;
					if (next <= musicIndexLimit) {
						float vol_next = MathUtils.clamp(volOut[next] + decimal * trackVolumes[next], 0, 1);
						// Gdx.app.log("PlayerTensiveMusic", "vol_next=" + vol_next);

						volTrack[next].set(vol_next, alpha_inc_next);
					}
				} else {
					volTrack[i].set(0, alpha_dec);
				}

				// interpolate and set
				volOut[i] = volTrack[i].get() * 1f * SoundManager.MusicVolumeMul;
				setVolume(i, volOut[i]);
			}
		}
	}
}
