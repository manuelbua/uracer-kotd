package com.bitfire.uracer.postprocessing.filters;

import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.postprocessing.PingPongBuffer;

public abstract class MultipassFilter extends IFilter {
	protected PingPongBuffer buffer = null;

	public abstract void render( PingPongBuffer srcdest );
}
