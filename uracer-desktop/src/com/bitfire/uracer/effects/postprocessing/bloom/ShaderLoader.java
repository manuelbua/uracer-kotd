package com.bitfire.uracer.effects.postprocessing.bloom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class ShaderLoader {

	static final public ShaderProgram createShader( String vertexName, String fragmentName )
	{
		String vertexShader = Gdx.files.internal( "data/shaders/" + vertexName + ".vertex" ).readString();
		String fragmentShader = Gdx.files.internal( "data/shaders/" + fragmentName + ".fragment" ).readString();
		ShaderProgram.pedantic = false;
		ShaderProgram shader = new ShaderProgram( vertexShader, fragmentShader );
		if( !shader.isCompiled() )
		{
			System.out.println( shader.getLog() );
			Gdx.app.exit();
		} //else
//			Gdx.app.log( "shader compiled", shader.getLog() );

		return shader;
	}
}
