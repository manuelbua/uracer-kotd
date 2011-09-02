package com.bitfire.uracer.entities.vehicles;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.entities.EntityState;
import com.bitfire.uracer.simulations.car.CarDescriptor;
import com.bitfire.uracer.utils.Convert;

public class CarGraphics
{
	private Sprite facet;
	private Sprite ambientOcclusion;

	public CarGraphics( CarDescriptor descriptor, TextureRegion region )
	{
		// aspect
		facet = new Sprite();
		facet.setRegion( region );
		facet.setSize( Convert.mt2px( descriptor.carModel.width ), Convert.mt2px( descriptor.carModel.length ) );
		facet.setOrigin( facet.getWidth() / 2, facet.getHeight() / 2 );

		// ambient occlusion
		ambientOcclusion = new Sprite();
		ambientOcclusion.setRegion( Art.carAmbientOcclusion );
		ambientOcclusion.setSize( facet.getWidth(), facet.getHeight() );
		ambientOcclusion.setScale( 2.25f, 2.3f );
		ambientOcclusion.setOrigin( ambientOcclusion.getWidth()/2, ambientOcclusion.getHeight()/2 );
	}

	public void render( SpriteBatch batch, EntityState state )
	{
		batch.enableBlending();

		ambientOcclusion.setPosition( state.position.x - ambientOcclusion.getOriginX(), state.position.y - ambientOcclusion.getOriginY() );
		ambientOcclusion.setRotation( state.orientation );
		ambientOcclusion.draw( batch, 0.35f );

		facet.setPosition( state.position.x - facet.getOriginX(), state.position.y - facet.getOriginY() );
		facet.setRotation( state.orientation );
		facet.draw( batch );

		batch.disableBlending();
	}
}
