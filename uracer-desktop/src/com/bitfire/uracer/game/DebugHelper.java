package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.rendering.debug.Stats;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public final class DebugHelper {

	// rendering
	private GameWorldRenderer worldRenderer;
	private PostProcessor postProcessor;

	// player
	private static PlayerCar player;

	// frame stats
	private long frameStart;
	private float physicsTime, renderTime;
	private Stats gfxStats;
	private String uRacerInfo;

	// box2d
	private Box2DDebugRenderer b2drenderer;
	private World box2dWorld;

	private final GameRendererEvent.Listener onRender = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			switch( type ) {
			case BatchDebug:
				render( GameEvents.gameRenderer.batch );
				break;
			case Debug:
				if( Config.Debug.RenderBox2DWorldWireframe ) {
					renderB2dWorld( box2dWorld, worldRenderer.getOrthographicMvpMt() );
				}
				break;
			}

		}
	};

	public DebugHelper( GameWorldRenderer worldRenderer, World box2dWorld, PostProcessor postProcessor ) {
		this.worldRenderer = worldRenderer;
		this.box2dWorld = box2dWorld;
		this.postProcessor = postProcessor;

		GameEvents.gameRenderer.addListener( onRender, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT );
		GameEvents.gameRenderer.addListener( onRender, GameRendererEvent.Type.Debug, GameRendererEvent.Order.DEFAULT );

		player = null;
		physicsTime = 0;
		renderTime = 0;
		b2drenderer = new Box2DDebugRenderer();
		frameStart = System.nanoTime();

		// extrapolate version information
		uRacerInfo = URacer.getVersionInfo();

		// compute graphics stats size
		float updateHz = 24f;
		if( !Config.isDesktop ) {
			updateHz = 1f;
		}

		gfxStats = new Stats( updateHz );
	}

	public void dispose() {
		b2drenderer.dispose();
		gfxStats.dispose();
	}

	public static void setPlayer( PlayerCar player ) {
		DebugHelper.player = player;
	}

	public void tick() {
		gfxStats.update();

		long time = System.nanoTime();

		if( time - frameStart > 1000000000 ) {
			physicsTime = URacer.getPhysicsTime();
			renderTime = URacer.getRenderTime();
			frameStart = time;
		}
	}

	private void render( SpriteBatch batch ) {
		renderVersionInfo( batch );

		if( Config.Debug.RenderDebugInfoGraphics ) {
			renderGraphicalStats( batch, Gdx.graphics.getWidth() - gfxStats.getWidth(), Gdx.graphics.getHeight() - gfxStats.getHeight() - Art.DebugFontHeight - 5 );
		}

		if( Config.Debug.RenderDebugInfoMemoryStats ) {
			renderMemoryUsage( batch );
		}

		if( Config.Debug.RenderDebugInfoFpsStats ) {
			renderFpsStats( batch );
		}

		if( Config.Debug.RenderPlayerDebugInfo && player != null ) {
			renderPlayerInfo( batch, player );
		}

		if( Config.Debug.RenderDebugInfoPostProcessor && postProcessor != null ) {
			renderPostProcessorInfo( batch, postProcessor );
		}

		if( Config.Debug.RenderDebugInfoMeshStats ) {
			SpriteBatchUtils.drawString( batch, "total meshes=" + GameWorld.TotalMeshes, 0, Gdx.graphics.getHeight() - 14 );
			SpriteBatchUtils.drawString( batch, "rendered meshes=" + (GameWorldRenderer.renderedTrees + GameWorldRenderer.renderedWalls) + ", trees="
					+ GameWorldRenderer.renderedTrees + ", walls=" + GameWorldRenderer.renderedWalls + ", culled=" + GameWorldRenderer.culledMeshes, 0,
					Gdx.graphics.getHeight() - Art.DebugFontHeight );
		}
	}

	private void renderGraphicalStats( SpriteBatch batch, int x, int y ) {
		batch.draw( gfxStats.getRegion(), x, y );
	}

	private void renderFpsStats( SpriteBatch batch ) {
		String text = "fps: " + NumberString.formatLong( Gdx.graphics.getFramesPerSecond() ) + ", physics: " + NumberString.formatLong( physicsTime ) + ", graphics: "
				+ NumberString.formatLong( renderTime );

		SpriteBatchUtils.drawString( batch, text, Gdx.graphics.getWidth() - text.length() * Art.DebugFontWidth, Gdx.graphics.getHeight() - Art.DebugFontHeight );
	}

	private void renderVersionInfo( SpriteBatch batch ) {
		SpriteBatchUtils.drawString( batch, uRacerInfo, Gdx.graphics.getWidth() - uRacerInfo.length() * Art.DebugFontWidth, 0, Art.DebugFontWidth,
				Art.DebugFontHeight * 2 );
	}

	private void renderMemoryUsage( SpriteBatch batch ) {
		float oneOnMb = 1f / 1048576f;
		float javaHeapMb = (float)Gdx.app.getJavaHeap() * oneOnMb;
		float nativeHeapMb = (float)Gdx.app.getNativeHeap() * oneOnMb;

		String text = "java heap = " + NumberString.format( javaHeapMb ) + "MB" + " - native heap = " + NumberString.format( nativeHeapMb ) + "MB";

		SpriteBatchUtils.drawString( batch, text, (Gdx.graphics.getWidth() - text.length() * Art.DebugFontWidth) / 2, Gdx.graphics.getHeight() - Art.DebugFontHeight );
	}

	private void renderB2dWorld( World world, Matrix4 modelViewProj ) {
		b2drenderer.render( world, modelViewProj );
	}

	private void renderPostProcessorInfo( SpriteBatch batch, PostProcessor postProcessor ) {
		String text = "Post-processing effects count =" + postProcessor.getEnabledEffectsCount();
		SpriteBatchUtils.drawString( batch, text, 0, 128 );
	}

	private void renderPlayerInfo( SpriteBatch batch, PlayerCar player ) {
		CarDescriptor carDesc = player.getCarDescriptor();
		Body body = player.getBody();
		Vector2 pos = GameRenderer.ScreenUtils.screenPosForMt( body.getPosition() );
		EntityRenderState state = player.state();

		SpriteBatchUtils.drawString( batch, "vel_wc len =" + carDesc.velocity_wc.len(), 0, 13 );
		SpriteBatchUtils.drawString( batch, "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
		SpriteBatchUtils.drawString( batch, "steerangle=" + carDesc.steerangle, 0, 27 );
		SpriteBatchUtils.drawString( batch, "throttle=" + carDesc.throttle, 0, 34 );
		SpriteBatchUtils.drawString( batch, "screen x=" + pos.x + ",y=" + pos.y, 0, 80 );
		SpriteBatchUtils.drawString( batch, "world-mt x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, 87 );
		SpriteBatchUtils.drawString( batch, "world-px x=" + Convert.mt2px( body.getPosition().x ) + ",y=" + Convert.mt2px( body.getPosition().y ), 0, 93 );
		// Debug.drawString( "dir worldsize x=" + Director.worldSizeScaledPx.x + ",y=" +
		// Director.worldSizeScaledPx.y, 0, 100 );
		// Debug.drawString( "dir bounds x=" + Director.boundsPx.x + ",y=" + Director.boundsPx.width, 0, 107 );
		SpriteBatchUtils.drawString( batch, "orient=" + body.getAngle(), 0, 114 );
		SpriteBatchUtils.drawString( batch, "render.interp=" + (state.position.x + "," + state.position.y), 0, 121 );

		// BatchUtils.drawString( batch, "on tile " + tilePosition, 0, 0 );
	}
}
