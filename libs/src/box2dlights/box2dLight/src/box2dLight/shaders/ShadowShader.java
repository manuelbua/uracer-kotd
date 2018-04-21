package box2dLight.shaders;

import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class ShadowShader {
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
		final String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define MED " + RayHandler.getColorPrecision() + "\n" + "precision "
				+ RayHandler.getColorPrecision()
				+ " float;\n" //
				+ "#else\n" + "#define MED \n" + "#endif\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform MED sampler2D u_texture;\n" //
				+ "uniform MED vec4 ambient;\n" + "void main()\n"//
				+ "{\n" //
				+ "vec4 v_c = texture2D(u_texture, v_texCoords);\n" + "v_c.rgb = ambient.rgb + v_c.rgb* v_c.a;\n"//
				+ "v_c.a = ambient.a - v_c.a;\n"//
				+ "gl_FragColor = v_c;\n"//
				+ "}\n";
		ShaderProgram.pedantic = false;
		ShaderProgram shadowShader = new ShaderProgram( vertexShader, fragmentShader );
		if( shadowShader.isCompiled() == false ) {
			Gdx.app.log( "ERROR", shadowShader.getLog() );

		}

		return shadowShader;
	}
}
