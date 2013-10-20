
package com.bitfire.uracer.game.debug.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public class DebugMeter implements Disposable {
	// graphics data
	private Pixmap pixels;
	private Texture texture;
	private TextureRegion region;

	private int width, height;
	private float value, minValue, maxValue;
	private String name;
	private Vector2 pos;
	private boolean showLabel;

	public Color color = new Color(1, 1, 1, 1);

	public DebugMeter (int width, int height) {
		assert (width < 256 && height < 256);

		this.showLabel = true;
		this.name = "";
		this.width = width;
		this.height = height;
		this.pos = new Vector2();

		pixels = new Pixmap(this.width, this.height, Format.RGBA8888);
		texture = new Texture(width, height, Format.RGBA8888);
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		region = new TextureRegion(texture, 0, 0, pixels.getWidth(), pixels.getHeight());
	}

	@Override
	public void dispose () {
		texture.dispose();
		pixels.dispose();
	}

	public float getValue () {
		return value;
	}

	public void setValue (float value) {
		this.value = value;
	}

	public void setName (String name) {
		this.name = name + " = ";
	}

	public void setLimits (float min, float max) {
		minValue = min;
		maxValue = max;
	}

	public void setShowLabel (boolean show) {
		this.showLabel = show;
	}

	public int getWidth () {
		return width;
	}

	public int getHeight () {
		return height;
	}

	public String getMessage () {
		return name + String.format("%.04f", Math.abs(value));
	}

	public void setPosition (Vector2 position) {
		pos.set(position);
	}

	public void setPosition (float x, float y) {
		pos.set(x, y);
	}

	public void render (SpriteBatch batch) {
		drawMeter();
		if (showLabel) {
			SpriteBatchUtils.drawString(batch, getMessage(), (int)pos.x, (int)pos.y);
		}
		batch.draw(region, (int)pos.x, (int)pos.y + (showLabel ? Art.DebugFontHeight : 0));
	}

	private void drawMeter () {
		pixels.setColor(0.25f, 0.25f, 0.25f, color.a);
		pixels.fill();

		float range = maxValue - minValue;
		float ratio = Math.abs(value) / range;
		ratio = AMath.clamp(ratio, 0, 1);

		pixels.setColor(color);
		pixels.fillRectangle(1, 1, (int)(width * ratio) - 2, height - 2);
		texture.draw(pixels, 0, 0);
	}
}
