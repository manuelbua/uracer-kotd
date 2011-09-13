package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.GameplaySettings;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.RadialBlur;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.utils.Convert;

public class CarTestScreen extends Screen
{
	private Car car = null, other = null;
	private Level level;
	// private GhostCar ghost;

	// test
	// private TestTilemap tm;
	private Vector2 carStartPos = new Vector2();
	private Vector2 otherStartPos = new Vector2();
	// private Vector2 replayCarStartPos = new Vector2();
	// private float replayCarStartOrient;
	private RadialBlur rb;

	public CarTestScreen()
	{
		ShaderProgram.pedantic = false;

		OrthographicAlignedStillModel.initialize();
		EntityManager.create();
		ModelFactory.init();

		Director.create( this, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );

		GameplaySettings gs = GameplaySettings.create( GameplaySettings.Easy );
		level = Director.loadLevel( "level1", gs );
		Director.setPositionPx( Director.positionFor( new Vector2(0,0)), false );

		carStartPos.set( Convert.tileToPx( 1, 0 ).add( Convert.scaledPixels( 112, -112 ) ) );
		otherStartPos.set( Convert.tileToPx( 3, 0 ).add( Convert.scaledPixels( 112, -112 ) ) );

		CarModel m = new CarModel();
		car = CarFactory.create( CarFactory.OldSkool, m.toModel2(), carStartPos, 90, true );
		other = CarFactory.create( CarFactory.OldSkool2, m.toModel1(), otherStartPos, 90, false );

		// car.record( true );
		// ghost = GhostCar.create( Convert.scaledPosition( 0, 0 ), 90 );

		if( Config.EnablePostProcessingFx )
		{
			rb = new RadialBlur();
			rb.setEnabled( true );
			PostProcessor.init( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
//			PostProcessor.init( 512, 512 );
			PostProcessor.setEffect( rb );
		}
	}

	@Override
	public void removed()
	{
		super.removed();
		Debug.dispose();
		Art.dispose();
	}

	private Vector2 carTileAt = new Vector2();
	private Vector2 lastCarTileAt = new Vector2();

	@Override
	public void tick()
	{
		if( Input.isOn( Keys.R ) )
		{
			// ghost.resetPhysics();
			if(car != null )
			{
				car.resetPhysics();
				car.setTransform( carStartPos, 90f );
			}

			if(other!=null)
			{
				other.resetPhysics();
				other.setTransform( otherStartPos, 90f );
			}
		}

		// Vector3 pos = Director.pos();
		// if( Input.isOn( Keys.UP ) ) pos.y += 10;
		// if( Input.isOn( Keys.DOWN ) ) pos.y -= 10;
		// if( Input.isOn( Keys.LEFT ) ) pos.x -= 10;
		// if( Input.isOn( Keys.RIGHT ) ) pos.x += 10;

		EntityManager.raiseOnTick();

		//
		// ubersimple events dispatcher
		//
		if(car!=null)
		{
			lastCarTileAt.set( carTileAt );
			carTileAt.set( Convert.pxToTile( car.pos().x, car.pos().y ) );
			if( (lastCarTileAt.x != carTileAt.x) || (lastCarTileAt.y != carTileAt.y) )
			{
				onTileChanged( carTileAt );
			}

			if( Config.EnablePostProcessingFx )
			{
				rb.dampStrength( 0.9f, Physics.dt );
				rb.setOrigin( Director.screenPosFor( car.getBody() ) );
			}
		}
	}

	// FIXME
	// shit, shouldn't this be subscribed instead, and not self-raised like
	// this?
	//
	// private boolean doRecord = true;
	protected void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		if( onStartZone )
		{
			// if(doRecord)
			// {
			// System.out.println("Recording...");
			// replayCarStartPos.set( car.pos() );
			// replayCarStartOrient = car.orient();
			// car.record( true );
			// }
			// else
			// {
			// System.out.println("Playing...");
			// car.record( false );
			// ghost.resetPhysics();
			// ghost.setReplay( car.getReplay(), replayCarStartPos,
			// replayCarStartOrient, car.carDesc );
			// }
			//
			// doRecord = !doRecord;
		}
	}

	@Override
	public void beforeRender( float timeAliasingFactor )
	{
		EntityManager.raiseOnBeforeRender( timeAliasingFactor );
	}

	private void renderScene( GL20 gl, float temporalAliasingFactor )
	{
		gl.glClearDepthf( 1 );
		gl.glClearColor( 0, 0, 0, 1 );
		gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );

		level.syncWithCam( Director.getCamera() );
		level.renderTilemap();
		EntityManager.raiseOnRender( temporalAliasingFactor );
		level.renderMeshes( gl );
	}

	@Override
	public void render( float temporalAliasingFactor )
	{
		GL20 gl = Gdx.graphics.getGL20();

		// follow the car
		if(car!=null)
			Director.setPositionPx( car.state().position, false );

		if( Config.EnablePostProcessingFx )
		{
			PostProcessor.begin();
			renderScene( gl, temporalAliasingFactor );
			PostProcessor.end();
		} else
		{
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			renderScene( gl, temporalAliasingFactor );
		}

		//
		// debug
		//

		if( Gdx.app.getType() == ApplicationType.Desktop )
		{
			Debug.renderB2dWorld( Director.getMatViewProjMt() );
		}

		Debug.begin();
		EntityManager.raiseOnDebug();
		Debug.renderVersionInfo();
		Debug.renderMemoryUsage();
		Debug.renderFrameStats( temporalAliasingFactor );
		Debug.drawString( "EMgr::maxSpritesInBatch = " + EntityManager.maxSpritesInBatch(), 0, 6 );
		Debug.drawString( "EMgr::renderCalls = " + EntityManager.renderCalls(), 0, 12 );
		Debug.end();
	}
}
