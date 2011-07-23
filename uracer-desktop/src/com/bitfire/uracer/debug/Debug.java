package com.bitfire.uracer.debug;

import java.util.Formatter;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.screen.Screen;

public class Debug
{
	private Screen attachedTo;

	private StringBuilder sb;
	private Formatter fmt;

	// frame stats
	private long frameStart = System.nanoTime();
	private float physicsTime, renderTime;

	// box2d
	private Box2DDebugRenderer20 b2drenderer;

	public Debug( Screen attachedTo )
	{
		this.attachedTo = attachedTo;
		physicsTime = renderTime = 0;
		b2drenderer = new Box2DDebugRenderer20();

		sb = new StringBuilder();
		fmt = new Formatter(sb, Locale.US);
	}


	public void dispose()
	{
		b2drenderer.dispose();
	}


	public void renderFrameStats( float temporalAliasingFactor )
	{
		long time = System.nanoTime();

		if( time - frameStart > 1000000000 )
		{
			physicsTime = URacer.getPhysicsTime();
			renderTime = URacer.getRenderTime();
			frameStart = time;
		}

		sb.setLength( 0 );
		attachedTo.drawString( fmt.format(
			"fps: %d, physics: %.06f, graphics: %.06f",
			Gdx.graphics.getFramesPerSecond(),
			physicsTime,
			renderTime).toString()
		, 0, 0 );

		sb.setLength( 0 );
		attachedTo.drawString( fmt.format(
			"timemul: x%.02f, step: %.0fHz",
			Physics.timeMultiplier,
			Physics.timestepHz).toString()
		, 0, 6 );
}


	public void renderB2dWorld( Matrix4 modelViewProj )
	{
		b2drenderer.render( modelViewProj, Physics.world );
	}
}
