package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.utils.AMath;

public class PostProcessEffect
{
	protected float MaxStrength;
	protected float effectStrength;
	protected ShaderProgram shader;
	protected boolean enabled;

	public PostProcessEffect()
	{
		shader = null;
		enabled = false;
		MaxStrength = 1f;
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

	public float getStrength()
	{
		return effectStrength;
	}

	public void setStrength( float aStrength )
	{
		effectStrength = AMath.clamp( aStrength, 0, MaxStrength );
	}

	public void addStrength( float aStrength )
	{
		setStrength( effectStrength + aStrength );
	}

	public void dampStrength( float factor, float dt )
	{
		// TODO: could pow be NOT used instead? this is a *fixed* timestep after all..
		// if this is going to be called from within the fixed timestep event then it could
		// be safe to remove it!
		setStrength( effectStrength * (float)Math.pow( (1.0f-factor), dt ) );
	}
}
