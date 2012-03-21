package com.bitfire.uracer.postprocessing;

/** The base class for any filter, the fullscreen quad resource is
 * shared between all instances.
 * 
 * @author bmanuel */
public abstract class IFilter {
	public static final FullscreenQuad quad = new FullscreenQuad();
}