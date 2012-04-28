package com.bitfire.uracer.game.rendering.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.Director;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public final class Debug {

	// player
	private PlayerCar player;

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
			}
		}
	};

	public Debug( World box2dWorld ) {
		GameEvents.gameRenderer.addListener( onRender, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.PLUS_4 );
		player = null;
		physicsTime = 0;
		renderTime = 0;
		b2drenderer = new Box2DDebugRenderer();
		frameStart = System.nanoTime();
		this.box2dWorld = box2dWorld;

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

	public void setPlayer( PlayerCar player ) {
		this.player = player;
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

		if( Config.Graphics.RenderBox2DWorldWireframe ) {
			renderB2dWorld( box2dWorld, Director.getMatViewProjMt() );
		}

		if( Config.Graphics.RenderDebugInfoGraphics ) {
			renderGraphicalStats( batch, Gdx.graphics.getWidth() - getStatsWidth(), Gdx.graphics.getHeight() - getStatsHeight() - Art.DebugFontHeight - 5 );
		}

		if( Config.Graphics.RenderDebugInfoMemoryStats ) {
			renderMemoryUsage( batch );
		}

		if( Config.Graphics.RenderDebugInfoFpsStats ) {
			renderFpsStats( batch );
		}

		if( player != null && Config.Graphics.RenderPlayerDebugInfo ) {
			renderPlayerInfo( batch, player );
		}

		if( Config.Graphics.RenderDebugInfoMeshStats ) {
			SpriteBatchUtils.drawString( batch, "total meshes=" + GameWorld.TotalMeshes, 0, Gdx.graphics.getHeight() - 14 );
			SpriteBatchUtils.drawString( batch, "rendered meshes=" + (GameWorldRenderer.renderedTrees + GameWorldRenderer.renderedWalls) + ", trees="
					+ GameWorldRenderer.renderedTrees + ", walls=" + GameWorldRenderer.renderedWalls + ", culled=" + GameWorldRenderer.culledMeshes, 0,
					Gdx.graphics.getHeight() - 7 );
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

	private void renderPlayerInfo( SpriteBatch batch, PlayerCar player ) {
		CarDescriptor carDesc = player.getCarDescriptor();
		Body body = player.getBody();
		EntityRenderState state = player.state();

		SpriteBatchUtils.drawString( batch, "vel_wc len =" + carDesc.velocity_wc.len(), 0, 13 );
		SpriteBatchUtils.drawString( batch, "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, 20 );
		SpriteBatchUtils.drawString( batch, "steerangle=" + carDesc.steerangle, 0, 27 );
		SpriteBatchUtils.drawString( batch, "throttle=" + carDesc.throttle, 0, 34 );
		SpriteBatchUtils.drawString( batch, "screen x=" + Director.screenPosFor( body ).x + ",y=" + Director.screenPosFor( body ).y, 0, 80 );
		SpriteBatchUtils.drawString( batch, "world-mt x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, 87 );
		SpriteBatchUtils.drawString( batch, "world-px x=" + Convert.mt2px( body.getPosition().x ) + ",y=" + Convert.mt2px( body.getPosition().y ), 0, 93 );
		// Debug.drawString( "dir worldsize x=" + Director.worldSizeScaledPx.x + ",y=" +
		// Director.worldSizeScaledPx.y, 0, 100 );
		// Debug.drawString( "dir bounds x=" + Director.boundsPx.x + ",y=" + Director.boundsPx.width, 0, 107 );
		SpriteBatchUtils.drawString( batch, "orient=" + body.getAngle(), 0, 114 );
		SpriteBatchUtils.drawString( batch, "render.interp=" + (state.position.x + "," + state.position.y), 0, 121 );

		// BatchUtils.drawString( batch, "on tile " + tilePosition, 0, 0 );
	}

	private int getStatsWidth() {
		return gfxStats.getWidth();
	}

	private int getStatsHeight() {
		return gfxStats.getHeight();
	}
}
