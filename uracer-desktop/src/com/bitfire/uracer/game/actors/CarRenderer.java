package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;

public final class CarRenderer {
	private Sprite facet;
	private Sprite ambientOcclusion;
	private TextureRegion region;
	private float alpha;

	public CarRenderer( CarModel model, Aspect aspect ) {
		facet = new Sprite();
		ambientOcclusion = new Sprite();
		setAspect( aspect, model );
	}

	public void setAspect( Aspect aspect, CarModel model ) {
		this.region = Art.cars.findRegion( aspect.name );
		facet.setRegion( region );
		facet.setSize( Convert.mt2px( model.width ), Convert.mt2px( model.length ) );
		facet.setOrigin( facet.getWidth() / 2, facet.getHeight() / 2 );

		// ambient occlusion
		ambientOcclusion.setRegion( Art.carAmbientOcclusion );
		ambientOcclusion.setSize( facet.getWidth(), facet.getHeight() );
		ambientOcclusion.setScale( 2f, 2.3f );
		ambientOcclusion.setOrigin( ambientOcclusion.getWidth() / 2, ambientOcclusion.getHeight() / 2 );
	}

	public Sprite getFacet() {
		return facet;
	}

	public TextureRegion getTextureRegion() {
		return region;
	}

	public void setAlpha( float alpha ) {
		this.alpha = alpha;
	}

	public float getAlpha() {
		return alpha;
	}

	public void render( SpriteBatch batch, EntityRenderState state ) {
		ambientOcclusion.setPosition( state.position.x - ambientOcclusion.getOriginX(), state.position.y - ambientOcclusion.getOriginY() );
		ambientOcclusion.setRotation( state.orientation );
		ambientOcclusion.draw( batch, 0.65f * alpha );

		facet.setPosition( state.position.x - facet.getOriginX(), state.position.y - facet.getOriginY() );
		facet.setRotation( state.orientation );
		facet.draw( batch, alpha );
	}
}
