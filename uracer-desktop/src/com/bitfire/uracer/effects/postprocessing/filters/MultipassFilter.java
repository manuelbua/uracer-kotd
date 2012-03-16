package com.bitfire.uracer.effects.postprocessing.filters;

import com.bitfire.uracer.effects.postprocessing.IFilter;
import com.bitfire.uracer.effects.postprocessing.PingPongBuffer;

public abstract class MultipassFilter extends IFilter
{
	protected PingPongBuffer buffer = null;

	public abstract void render(PingPongBuffer srcdest);
}
