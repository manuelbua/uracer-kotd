package com.bitfire.uracer.utils;

public final class UUid {
	public static long incr = 0;

	public static final long get() {
		incr++;
		Long time = System.nanoTime() + incr;
		return Hash.RSHash( time.toString() );
	}

	private UUid() {
	}
}
