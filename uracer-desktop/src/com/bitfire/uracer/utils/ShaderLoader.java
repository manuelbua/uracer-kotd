package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class ShaderLoader {

	static final public ShaderProgram createShader( String vertexName, String fragmentName ) {
		return ShaderLoader.createShader( vertexName, fragmentName, "" );
	}

	static final public ShaderProgram createShader( String vertexName, String fragmentName, String defines ) {
		System.out.println( "Compiling " + vertexName + " | " + fragmentName + "..." );
		String vertexShader = Gdx.files.internal( "data/shaders/" + vertexName + ".vertex" ).readString();
		String fragmentShader = Gdx.files.internal( "data/shaders/" + fragmentName + ".fragment" ).readString();
		ShaderProgram.pedantic = false;
		ShaderProgram shader = new ShaderProgram( defines + "\n" + vertexShader, defines + "\n" + fragmentShader );
		if( !shader.isCompiled() ) {
			System.out.println( shader.getLog() );
			Gdx.app.exit();
		}
		else {
			if( defines != null && defines.length() > 0 )
				System.out.println( vertexName + "/" + fragmentName + " compiled w/ (" + defines.replace( "\n", ", " ) + ")" );
			else
				System.out.println( vertexName + "/" + fragmentName + " compiled!" );
		}

		return shader;
	}
}
