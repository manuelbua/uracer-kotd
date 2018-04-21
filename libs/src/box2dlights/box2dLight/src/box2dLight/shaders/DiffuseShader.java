package box2dLight.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class DiffuseShader {
	static final public ShaderProgram createShadowShader() {
		final String vertexShader = "attribute vec4 a_position;\n" //
				+ "attribute vec2 a_texCoord;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_texCoords = a_texCoord;\n" //
				+ "   gl_Position = a_position;\n" //
				+ "}\n";

		// this is allways perfect precision
		final String fragmentShader = "#ifdef GL_ES\n" //
				+ "precision mediump float;\n" //
				+ "#endif\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "uniform  vec4 ambient;\n"
					+ "void main()\n"//
				+ "{\n" //
				+ "gl_FragColor = ambient + texture2D(u_texture, v_texCoords);\n"
					+ "}\n";
		ShaderProgram.pedantic = false;
		ShaderProgram shadowShader = new ShaderProgram(vertexShader,
					fragmentShader);
		if (shadowShader.isCompiled() == false) {
			Gdx.app.log("ERROR", shadowShader.getLog());

		}

		return shadowShader;
	}

}
