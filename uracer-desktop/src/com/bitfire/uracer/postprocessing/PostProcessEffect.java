package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class PostProcessEffect
{
	protected ShaderProgram shader;
	protected boolean enabled;

	public PostProcessEffect()
	{
		shader = null;
	}

	protected boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	protected void onBeforeShaderPass() {}

	public ShaderProgram getShader()
	{
		return shader;
	}
}
