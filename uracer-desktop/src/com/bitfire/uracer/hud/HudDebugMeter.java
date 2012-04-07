package com.bitfire.uracer.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public class HudDebugMeter {
	// graphics data
	private Pixmap pixels;
	private Texture texture;
	private TextureRegion region;

	private int width, height;
	private float value, minValue, maxValue;
	private String name;

	private Vector2 pos;
	private int row;
	private Car playerCar;

	public Color color = new Color( 1, 1, 1, 1 );

	public HudDebugMeter( Car car, int row, int width, int height ) {
		assert (width < 256 && height < 256);

		this.name = "";
		this.width = width;
		this.height = height;
		this.pos = new Vector2();
		this.row = row;
		playerCar = car;

		pixels = new Pixmap( this.width, this.height, Format.RGBA8888 );
		texture = new Texture( 256, 256, Format.RGBA8888 );
		texture.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
		region = new TextureRegion( texture, 0, 0, pixels.getWidth(), pixels.getHeight() );
	}

	// TODO dispose!!!
	public void setValue( float value ) {
		this.value = value;
	}

	public void setName( String name ) {
		this.name = name + " = ";
	}

	public void setLimits( float min, float max ) {
		minValue = min;
		maxValue = max;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getMessage() {
		return name + String.format( "%.04f", Math.abs( value ) );
	}

	private void update() {
		pos.set( Director.screenPosForPx( playerCar.state().position ) );
		// pos.set( Director.screenPosFor( playerCar.getBody() ) );

		// center horizontally
		pos.x -= width * 0.5f;

		// offset by half car length
		pos.y += Convert.mt2px( playerCar.getCarModel().length ) * 0.5f;

		// offset by row
		pos.y += row * (height + Debug.fontHeight);
	}

	public void render( SpriteBatch batch ) {
		update();
		draw();
		Debug.drawString( batch, getMessage(), pos.x, pos.y );

		batch.draw( region, pos.x, pos.y + Debug.fontHeight );
	}

	private void draw() {
		pixels.setColor( 0, 0, 0, 1 );
		pixels.fill();

		float range = maxValue - minValue;
		float ratio = Math.abs( value ) / range;
		ratio = AMath.clamp( ratio, 0, 1 );

		pixels.setColor( color );
		pixels.fillRectangle( 1, 1, (int)(width * ratio) - 2, height - 2 );

		texture.draw( pixels, 0, 0 );
	}
}
