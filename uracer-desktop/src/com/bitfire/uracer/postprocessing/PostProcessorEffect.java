package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

/** This interface defines the base class for the concrete implementation
 * of post-processor effects.
 * 
 * @author bmanuel */
public abstract class PostProcessorEffect implements Disposable {
	public final int id;
	public final String name;
	public final PostProcessor postProcessor;

	/** Construct a new post-processor effect, computing
	 * the hash code by its class name. */
	public PostProcessorEffect( PostProcessor postProcessor ) {
		this.name = this.getClass().getSimpleName();
		this.postProcessor = postProcessor;
		this.id = this.name.hashCode();
	}

	/** Returns the custom hashcode. */
	@Override
	public int hashCode() {
		return this.id;
	}

	/** Returns whether or not this instance is equal to the other by
	 * comparing the ids (hashcodes). */
	@Override
	public boolean equals( Object obj ) {
		if( this == obj )
			return true;
		if( obj == null )
			return false;
		if( getClass() != obj.getClass() )
			return false;

		PostProcessorEffect e = (PostProcessorEffect)obj;
		return e.id == this.id;
	};

	/** Concrete objects shall be responsible to recreate or rebind its own
	 * resources whenever its needed, usually when the OpenGL context
	 * is lost.
	 * Eg., framebuffers' texture should be updated and shader parameters
	 * should be reuploaded. */
	public abstract void rebind();

	/** Concrete objects shall implements its own rendering, given the
	 * source and destination buffers. */
	public abstract void render( final FrameBuffer src, final FrameBuffer dest );
}