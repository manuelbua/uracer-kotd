package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.postprocessing.IFilter;

// @SuppressWarnings( "unchecked" )
public abstract class Filter<T> extends IFilter {
	protected static final int u_texture_1 = 0;
	protected static final int u_texture_2 = 1;

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

	public void setParam( Parameter param, float value ) {
		program.begin();
		program.setUniformf( param.mnemonic(), value );
		program.end();
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

	// int
	public T setParams( Parameter param, int value ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}
		program.setUniformi( param.mnemonic(), value );
		return (T)this;
	}

	// float[], vec2[], vec3[], vec4[]
	public T setParamsv( Parameter param, float[] values, int offset, int length ) {
		if( !programBegan ) {
			programBegan = true;
			program.begin();
		}

		switch( param.arrayElementSize() ) {
		default:
		case 1:
			program.setUniform1fv( param.mnemonic(), values, offset, length );
			break;
		case 2:
			program.setUniform2fv( param.mnemonic(), values, offset, length );
			break;
		case 3:
			program.setUniform3fv( param.mnemonic(), values, offset, length );
			break;
		case 4:
			program.setUniform4fv( param.mnemonic(), values, offset, length );
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
		} else
			compute();
	}
}
