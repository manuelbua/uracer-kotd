
package com.bitfire.uracer.utils;

/** Typed runtime exception used throughout uracer */
public class URacerRuntimeException extends RuntimeException {
	public URacerRuntimeException (String message) {
		super(message);
	}

	public URacerRuntimeException (Throwable t) {
		super(t);
	}

	public URacerRuntimeException (String message, Throwable t) {
		super(message, t);
	}
}
