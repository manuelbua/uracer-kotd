
package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.game.logic.replaying.ReplayInfo;

public final class ReplayUtils {

	public static boolean areValidIds (ReplayInfo info) {
		if (info != null) {
			return DigestUtils.isValidDigest(info.getId()) && info.getUserId().length() > 0 && info.getTrackId().length() > 0;
		}

		return false;
	}

	public static String getDestinationDir (ReplayInfo info) {
		if (areValidIds(info)) {
			return Storage.ReplaysRoot + info.getTrackId() + "/" + info.getUserId() + "/";
		}

		return "";
	}

	public static String getFullPath (ReplayInfo info) {
		return getDestinationDir(info) + info.getId();
	}

	public static boolean pruneReplay (ReplayInfo info) {
		if (info != null && ReplayUtils.areValidIds(info)) {
			String rid = info.getId();
			if (rid.length() > 0) {
				String path = getFullPath(info);
				if (path.length() > 0) {
					FileHandle hf = Gdx.files.external(path);
					if (hf.exists()) {
						hf.delete();
						Gdx.app.log("ReplayUtils", "Pruned #" + rid);
						return true;
					} else {
						Gdx.app.error("ReplayUtils", "Couldn't prune #" + rid);
					}
				}
			}
		}

		return false;
	}

	public static int ticksToMilliseconds (int ticks) {
		return (int)(ticks * Config.Physics.Dt * AMath.ONE_ON_CMP_EPSILON);
	}

	public static float ticksToSeconds (int ticks) {
		return (float)ticksToMilliseconds(ticks) / 1000f;
	}
}
