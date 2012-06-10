package com.bitfire.uracer.postprocessing.filters;

import com.bitfire.uracer.postprocessing.PingPongBuffer;

public abstract class MultipassFilter {
	public abstract void render( PingPongBuffer srcdest );
}
