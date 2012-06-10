package com.bitfire.uracer.postprocessing.filters;

import com.bitfire.uracer.postprocessing.PingPongBuffer;

/** The base class for any multi-pass filter */
public abstract class MultipassFilter {
	public abstract void render( PingPongBuffer srcdest );
}
