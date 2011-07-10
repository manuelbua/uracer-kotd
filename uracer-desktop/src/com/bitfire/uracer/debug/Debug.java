package com.bitfire.uracer.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.screen.Screen;

public class Debug
{
	private Screen attachedTo;

	// frame stats
	private long frameStart = System.nanoTime();
	private float physicsTime = 0, renderTime = 0;

	// box2d
	private Box2DDebugRenderer20 b2drenderer;

	public Debug( Screen attachedTo )
	{
		this.attachedTo = attachedTo;
		b2drenderer = new Box2DDebugRenderer20();
	}


	public void dispose()
	{
		b2drenderer.dispose();
	}


	public void renderFrameStats( float timeAliasingFactor )
	{
		long time = System.nanoTime();

		if( time - frameStart > 1000000000 )
		{
			physicsTime = URacer.getPhysicsTime();
			renderTime = URacer.getRenderTime();
			frameStart = time;
		}

		attachedTo.drawString( "fps:" + Gdx.graphics.getFramesPerSecond() + ", physics: " + physicsTime + ", render: " + renderTime, 0, 130 );
	}


	public void renderB2dWorld( World world, Matrix4 modelViewProj )
	{
		b2drenderer.render( modelViewProj, world );
	}
}
