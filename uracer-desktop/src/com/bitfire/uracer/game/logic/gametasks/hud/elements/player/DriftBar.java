
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.utils.Convert;

public class DriftBar extends Positionable implements Disposable {
	public static final float MaxSeconds = 2f;
	public static final int MaxTicks = (int)(MaxSeconds * Config.Physics.PhysicsTimestepHz);

	private final float invTilemapZoom;
	private final float width, height, halfWidth, halfHeight;
	private final Pixmap pixels;
	private final Texture texture;
	private float seconds;
	private Color barColor;

	public DriftBar (float invTilemapZoomFactor, float width) {
		this.invTilemapZoom = invTilemapZoomFactor;
		this.width = width;
		this.height = Convert.scaledPixels(7);
		halfWidth = width / 2;
		halfHeight = height / 2;
		seconds = 0;

		barColor = new Color();
		pixels = new Pixmap((int)this.width, (int)this.height, Format.RGBA8888);
		texture = new Texture(pixels);
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
	}

	public void setSeconds (float seconds) {
		this.seconds = MathUtils.clamp(seconds, 0, MaxSeconds);
	}

	public float getSeconds () {
		return seconds;
	}

	@Override
	public void dispose () {
		texture.dispose();
		pixels.dispose();
	}

	public void render (SpriteBatch batch, float cameraZoom) {
		pixels.setColor(0, 0, 0, 1);
		pixels.fill();

		float ratio = seconds / MaxSeconds;
// ratio = 1f;

		int w = MathUtils.clamp((int)(width * ratio) - 2, 0, (int)width);
		int h = (int)height - 2;

		if (w > 0) {
			// full = 173|255|30 / 0.678 | 1 | 0.118
			// mid = 247|194|29 / 0.969 | 0.761 | 0.114
			// low = 247|30|29 / 0.969 | 0.118 | 0.114

			float greenRatio = MathUtils.clamp(ratio, 0.23f, 1);
			float rbRange = (1 - MathUtils.clamp(greenRatio, 0.761f, 1)) / (1 - 0.761f);

			barColor.r = 0.678f + (0.969f - 0.678f) * rbRange;
			barColor.g = greenRatio;
			barColor.b = 0.118f - (0.118f - 0.114f) * rbRange;

// Gdx.app.log("Color", "rbr=" + rbRange + " - " + barColor.r * 255 + "," + barColor.g * 255 + "," + barColor.b * 255);
			barColor.a = 1f;

			pixels.setColor(barColor);
			pixels.fillRectangle(1, 1, w, h);
		}

		texture.draw(pixels, 0, 0);

		batch.draw(texture, position.x - halfWidth * cameraZoom, position.y - halfHeight * cameraZoom, (int)(width * cameraZoom),
			(int)(height * cameraZoom));
// batch.draw(texture, position.x - halfWidth * cameraZoom, position.y - halfHeight * cameraZoom);
	}
}
