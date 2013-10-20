
package com.bitfire.uracer.utils;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.logic.replaying.Replay;

public class DigestUtils {

	public static final MessageDigest sha256;
	public static final String HardwareId;

	static {
		// try setup sha256 digest
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new URacerRuntimeException("No support for SHA-256 crypto has been found.");
		}

		// try retrieve macaddress(es)
		String mac = "";

		try {
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = nis.nextElement();
				byte[] ma = ni.getHardwareAddress();
				if (ma != null) {
					for (int i = 0; i < ma.length; i++) {
						mac += String.format("%02X", ma[i]);
					}
				}
			}
		} catch (SocketException e) {
			throw new URacerRuntimeException("Cannot determine the MAC address of this machine!" + e.getMessage());
		}

		if (mac.length() < 6) {
			throw new URacerRuntimeException("Cannot retrieve a valid MAC address for this machine!");
		}

		HardwareId = mac;
		Gdx.app.log("DigestUtils", "HardwareID set to 0x" + HardwareId);
	}

	public static final String computeDigest (Replay replay) {
		if (replay.isValidData()) {
			String trackTimeTicks = "" + replay.getTicks();
			String created = "" + replay.getCreationTimestamp();

			sha256.reset();
			sha256.update(HardwareId.getBytes());
			sha256.update(created.getBytes());
			sha256.update(replay.getUserId().getBytes());
			sha256.update(replay.getTrackId().getBytes());
			sha256.update(trackTimeTicks.getBytes());

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
