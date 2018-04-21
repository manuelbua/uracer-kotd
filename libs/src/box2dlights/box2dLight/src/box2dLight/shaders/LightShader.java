package box2dLight.shaders;

import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class LightShader {
	static final public ShaderProgram createLightShader() {
		String gamma = "";
		if (RayHandler.getGammaCorrection())
			gamma = "sqrt";

		final String vertexShader = "#ifdef GL_ES\n" //
				+ "#define MED "+ RayHandler.getColorPrecision() + "\n"
				+ "precision "+RayHandler.getColorPrecision()+" float;\n" //
				+ "#define PRES mediump\n"
				+ "#else\n"
				+ "#define MED \n"
				+ "#define PRES \n"
				+ "#endif\n" //
				+ "attribute MED vec4 vertex_positions;\n" //
				+ "attribute MED vec4 quad_colors;\n" //
				+ "attribute float s;\n"
				+ "uniform PRES mat4 u_projTrans;\n" //
				+ "varying MED vec4 v_color;\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = s * quad_colors;\n" //
				+ "   gl_Position =  u_projTrans * vertex_positions;\n" //
				+ "}\n";
		final String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define MED "+ RayHandler.getColorPrecision() + "\n"
				+ "precision "+RayHandler.getColorPrecision()+" float;\n" //
				+ "#else\n"
				+ "#define MED \n"
				+ "#endif\n" //
				+ "varying MED vec4 v_color;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "  gl_FragColor = "+gamma+"(v_color);\n" //
				+ "}";

		ShaderProgram.pedantic = false;
		ShaderProgram lightShader = new ShaderProgram(vertexShader,
				fragmentShader);
		if (lightShader.isCompiled() == false) {
			Gdx.app.log("ERROR", lightShader.getLog());
		}

		return lightShader;
	}
}
