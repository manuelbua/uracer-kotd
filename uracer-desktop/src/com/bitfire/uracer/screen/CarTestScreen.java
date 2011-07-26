package com.bitfire.uracer.screen;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.Car;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.simulation.CarContactListener;

public class CarTestScreen extends Screen
{
	private FPSLogger fpslog = new FPSLogger();
	private Debug dbg;
	private Car car;


	public CarTestScreen()
	{
		dbg = new Debug( this );
		Physics.create( new Vector2( 0, 0 ), false );
		Physics.world.setContactListener( new CarContactListener() );
		EntityManager.create();
		Director.createFromPixels( Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new Vector2( 0, 0 ) );
		Director.setPositionPx( new Vector2(Gdx.graphics.getWidth()/2f, Gdx.graphics.getHeight()/2f) );

		car = Car.create( Physics.px2mt(new Vector2(Gdx.graphics.getWidth()/2f,Gdx.graphics.getHeight()/2f)), true );
//		car.setOrientation( MathUtils.degreesToRadians * 2f );
	}

	@Override
	public void removed()
	{
		super.removed();
		dbg.dispose();
	}

	@Override
	public void tick()
	{
		EntityManager.raiseOnTick();

		if(Input.isOn( Keys.R ))
		{
			car.reset();
			car.setPosition( Physics.px2mt(new Vector2(Gdx.graphics.getWidth()/2f,Gdx.graphics.getHeight()/2f)) );
			car.setOrientation( 0 );
		}

//		if( Input.isTouching() )
//		{
//			Config.PhysicsTimeMultiplier = 0.1f;
//		} else
//		{
//			Config.PhysicsTimeMultiplier = 1f;
//		}
	}

	@Override
	public void render( float temporalAliasingFactor )
	{
		GL20 gl = Gdx.graphics.getGL20();
		gl.glClearColor( 0, 0, 0, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		Director.update();
		EntityManager.raiseOnRender( temporalAliasingFactor );

		// debug
		if( Gdx.app.getType() == ApplicationType.Desktop )
		{
			dbg.renderB2dWorld( Director.getWorldCam().combined );
		}

		batch.begin();
		dbg.renderFrameStats( temporalAliasingFactor );
		car.debug( this, batch );
		batch.end();

//		fpslog.log();
	}
}
