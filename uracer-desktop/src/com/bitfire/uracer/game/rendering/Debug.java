package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.rendering.debug.Stats;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public final class Debug {

	// frame stats
	private static long frameStart;
	private static float physicsTime, renderTime;
	private static Stats gfxStats;
	private static String uRacerInfo;

	// box2d
	private static Box2DDebugRenderer b2drenderer;

	private static final GameRendererEvent.Listener onRender = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			switch( type ) {
			case BatchDebug:
				Debug.render( GameEvents.gameRenderer.batch );
				break;
			}
		}
	};

	private Debug() {
	}

	public static void create() {
		GameEvents.gameRenderer.addListener( onRender, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.PLUS_4 );
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

		SpriteBatchUtils.drawString( batch, text, Gdx.graphics.getWidth() - text.length() * Art.fontWidth, Gdx.graphics.getHeight() - Art.fontHeight );
	}

	public static void renderVersionInfo( SpriteBatch batch ) {
		SpriteBatchUtils.drawString( batch, uRacerInfo, Gdx.graphics.getWidth() - uRacerInfo.length() * Art.fontWidth, 0, Art.fontWidth, Art.fontHeight * 2 );
	}

	public static void renderMemoryUsage( SpriteBatch batch ) {
		float oneOnMb = 1f / 1048576f;
		float javaHeapMb = (float)Gdx.app.getJavaHeap() * oneOnMb;
		float nativeHeapMb = (float)Gdx.app.getNativeHeap() * oneOnMb;

		String text = "java heap = " + NumberString.format( javaHeapMb ) + "MB" + " - native heap = " + NumberString.format( nativeHeapMb ) + "MB";

		SpriteBatchUtils.drawString( batch, text, (Gdx.graphics.getWidth() - text.length() * Art.fontWidth) / 2, 0 );
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

	private static void render( SpriteBatch batch ) {
		if( Config.isDesktop ) {
			if( Config.Graphics.RenderBox2DWorldWireframe ) {
				Debug.renderB2dWorld( GameData.Environment.b2dWorld, Director.getMatViewProjMt() );
			}

			Debug.renderVersionInfo( batch );
			Debug.renderGraphicalStats( batch, Gdx.graphics.getWidth() - Debug.getStatsWidth(), Gdx.graphics.getHeight() - Debug.getStatsHeight() - Art.fontHeight - 5 );
			Debug.renderTextualStats( batch );
			Debug.renderMemoryUsage( batch );
			SpriteBatchUtils.drawString( batch, "total meshes=" + GameWorld.TotalMeshes, 0, Gdx.graphics.getHeight() - 14 );
			SpriteBatchUtils.drawString( batch, "rendered meshes=" + (GameWorldRenderer.renderedTrees + GameWorldRenderer.renderedWalls) + ", trees="
					+ GameWorldRenderer.renderedTrees + ", walls=" + GameWorldRenderer.renderedWalls + ", culled=" + GameWorldRenderer.culledMeshes, 0,
					Gdx.graphics.getHeight() - 7 );

		} else {

			Debug.renderVersionInfo( batch );
			Debug.renderTextualStats( batch );
		}
	}
}
