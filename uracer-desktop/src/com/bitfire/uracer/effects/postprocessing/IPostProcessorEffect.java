package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;


public interface IPostProcessorEffect
{
	public void dispose();
	public void resume();
	public void render(Mesh fullScreenQuad, Texture originalScene);
	public Color getClearColor();
}
