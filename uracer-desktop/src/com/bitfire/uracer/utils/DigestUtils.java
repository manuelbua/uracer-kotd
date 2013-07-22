
package com.bitfire.uracer.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.bitfire.uracer.game.logic.replaying.Replay;

public class DigestUtils {

	public static final MessageDigest sha256;

	static {
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new URacerRuntimeException("No support for SHA-256 crypto has been found.");
		}
	}

	public static final String computeDigest (Replay replay) {
		if (replay.isValidData()) {
			String trackTime = "" + replay.getMilliseconds();
			String created = "" + replay.getCreationTimestamp();

			sha256.reset();
			sha256.update(created.getBytes());
			sha256.update(replay.getUserId().getBytes());
			sha256.update(replay.getTrackId().getBytes());
			sha256.update(trackTime.getBytes());

			byte[] digest = sha256.digest();
			String replayId = "";

			// output MUST be zero-padded
			for (Byte b : digest) {
				replayId += String.format("%02x", b);
			}

			return replayId;
		}

		return "";
	}

	public static boolean isValidDigest (String digest) {
		return digest != null && digest.length() == 64;
	}
}
