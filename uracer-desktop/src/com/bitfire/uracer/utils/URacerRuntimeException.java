
package com.bitfire.uracer.utils;

/** Typed runtime exception used throughout uracer */
public class URacerRuntimeException extends RuntimeException {
	public static final long serialVersionUID = 110779130456L;

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
