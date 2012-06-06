package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.postprocessing.IFilter;

@SuppressWarnings( "unchecked" )
public abstract class Filter<T> extends IFilter {
	protected static final int u_texture0 = 0;
	protected static final int u_texture1 = 1;
	protected static final int u_texture2 = 2;
	protected static final int u_texture3 = 3;

	protected Texture inputTexture = null;
	protected FrameBuffer outputBuffer = null;
	protected ShaderProgram program = null;
	private boolean programBegan = false;

	public Filter( ShaderProgram program ) {
		this.program = program;
	}

	public T setInput( Texture input ) {
		this.inputTexture = input;
		return (T)this;	// assumes T extends Filter
	}

	public T setInput( FrameBuffer input ) {
		this.inputTexture = input.getColorBufferTexture();
		return (T)this;
	}

	public T setOutput( FrameBuffer output ) {
		this.outputBuffer = output;
		return (T)this;
	}

	public void dispose() {
		program.dispose();
	}

	public abstract void rebind();

	protected abstract void compute();

	public interface Parameter {
		String mnemonic();

		int arrayElementSize();
	}

	/* Sets the parameter to the specified value for this filter.
	 * This is for one-off operations since the shader is being bound and unbound once per call: for
	 * a batch-ready version of this fuction see and use setParams instead. */

	// int
	public void setParam( Parameter param, int value ) {
		program.begin();
		program.setUniformi( param.mnemonic(), value );
		program.end();
	}

	// float
	public void setParam( Parameter param, float value ) {
		program.begin();
		program.setUniformf( param.mnemonic(), value );
		program.end();
	}

	// vec2
	public void setParam( Parameter param, Vector2 value ) {
		program.begin();
		program.setUniformf( param.mnemonic(), value );
		program.end();
	}

	// vec3
	public void setParam( Parameter param, Vector3 value ) {
		program.begin();
		program.setUniformf( param.mnemonic(), value );
		program.end();
	}

	/** Sets the parameter to the specified value for this filter.
	 * A single call OR chained function calls, for setParams methods, shall be ended by calling endParams() */

	// mat3
	public T setParams( Parameter param, Matrix3 value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformMatrix( param.mnemonic(), value );
		return (T)this;
	}

	// mat4
	public T setParams( Parameter param, Matrix4 value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformMatrix( param.mnemonic(), value );
		return (T)this;
	}

	// float
	public T setParams( Parameter param, float value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformf( param.mnemonic(), value );
		return (T)this;
	}

	// int version
	public T setParams( Parameter param, int value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformi( param.mnemonic(), value );
		return (T)this;
	}

	// vec2 version
	public T setParams( Parameter param, Vector2 value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformf( param.mnemonic(), value );
		return (T)this;
	}

	// vec3 version
	public T setParams( Parameter param, Vector3 value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformf( param.mnemonic(), value );
		return (T)this;
	}

	// float[], vec2[], vec3[], vec4[]
	public T setParamsv( Parameter param, float[] values, int offset, int length ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}

		switch( param.arrayElementSize() ) {
		case 4:
			program.setUniform4fv( param.mnemonic(), values, offset, length );
			break;
		case 3:
			program.setUniform3fv( param.mnemonic(), values, offset, length );
			break;
		case 2:
			program.setUniform2fv( param.mnemonic(), values, offset, length );
			break;
		default:
		case 1:
			program.setUniform1fv( param.mnemonic(), values, offset, length );
			break;
		}

		return (T)this;
	}

	public void endParams() {
		if( programBegan ) {
			program.end();
			programBegan = false;
		}
	}

	public void render() {
		if( outputBuffer != null ) {
			outputBuffer.begin();
			compute();
			outputBuffer.end();
		} else {
			compute();
		}
	}
}
