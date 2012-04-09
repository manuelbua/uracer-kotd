package com.bitfire.uracer.game.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.utils.BatchUtils;
import com.bitfire.uracer.utils.NumberString;

public final class Debug {

	// frame stats
	private static long frameStart;
	private static float physicsTime, renderTime;
	private static Stats gfxStats;
	private static String uRacerInfo;

	// box2d
	private static Box2DDebugRenderer b2drenderer;

	public static int fontWidth;
	public static int fontHeight;

	private Debug() {
	}

	public static void create() {
		fontWidth = 6;
		fontHeight = 6;
		physicsTime = 0;
		renderTime = 0;
		b2drenderer = new Box2DDebugRenderer();
		frameStart = System.nanoTime();

		// extrapolate version information
		uRacerInfo = URacer.getVersionInfo();

		// compute graphics stats size
		float updateHz = 0.2f;
		if( !Config.isDesktop ) {
			updateHz = 1f;
		}

		gfxStats = new Stats( updateHz );
	}

	public static void dispose() {
		b2drenderer.dispose();
		gfxStats.dispose();
	}

	public static void tick() {
		gfxStats.update();

		long time = System.nanoTime();

		if( time - frameStart > 1000000000 ) {
			physicsTime = URacer.getPhysicsTime();
			renderTime = URacer.getRenderTime();
			frameStart = time;
		}
	}

	public static void renderGraphicalStats( SpriteBatch batch, int x, int y ) {
		batch.draw( gfxStats.getRegion(), x, y );
	}

	public static void renderTextualStats( SpriteBatch batch ) {
		String text = "fps: " + NumberString.formatLong( Gdx.graphics.getFramesPerSecond() ) + ", physics: " + NumberString.formatLong( physicsTime ) + ", graphics: "
				+ NumberString.formatLong( renderTime );

		BatchUtils.drawString( batch, text, Gdx.graphics.getWidth() - text.length() * fontWidth, Gdx.graphics.getHeight() - fontHeight );
	}

	public static void renderVersionInfo( SpriteBatch batch ) {
		BatchUtils.drawString( batch, uRacerInfo, Gdx.graphics.getWidth() - uRacerInfo.length() * fontWidth, 0, fontWidth, fontHeight * 2 );
	}

	public static void renderMemoryUsage( SpriteBatch batch ) {
		float oneOnMb = 1f / 1048576f;
		float javaHeapMb = (float)Gdx.app.getJavaHeap() * oneOnMb;
		float nativeHeapMb = (float)Gdx.app.getNativeHeap() * oneOnMb;

		String text = "java heap = " + NumberString.format( javaHeapMb ) + "MB" + " - native heap = " + NumberString.format( nativeHeapMb ) + "MB";

		BatchUtils.drawString( batch, text, (Gdx.graphics.getWidth() - text.length() * fontWidth) / 2, 0 );
	}

	public static void renderB2dWorld( World world, Matrix4 modelViewProj ) {
		b2drenderer.render( world, modelViewProj );
	}

	public static int getStatsWidth() {
		return gfxStats.getWidth();
	}

	public static int getStatsHeight() {
		return gfxStats.getHeight();
	}
}
