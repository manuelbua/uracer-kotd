
package com.bitfire.uracer.game.logic.gametasks.hud.elements.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.Positionable;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory.FontFace;
import com.bitfire.uracer.utils.ColorUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.utils.ShaderLoader;

public class DriftBar extends Positionable implements Disposable {
	public static final float MaxSeconds = 10f;
	public static final int MaxTicks = (int)(MaxSeconds * Config.Physics.PhysicsTimestepHz);

	private final float scale;
	private final float width, height, halfWidth, halfHeight;
	private final Pixmap pixels;
	private final Texture texture;
	private float seconds;
	private Color barColor;
	private HudLabel labelSeconds;

	private final Texture texProgress, texMask;
	private final ShaderProgram shProgress;
	private final Sprite sProgress;
	private final float offX, offY, w, h;

	public DriftBar (float scale, float width) {
		this.scale = scale;
		this.width = width;
		this.height = Convert.scaledPixels(7);
		halfWidth = width / 2;
		halfHeight = height / 2;
		seconds = 0;

		barColor = new Color(1, 1, 1, 1);
		pixels = new Pixmap((int)this.width, (int)this.height, Format.RGBA8888);
		texture = new Texture(pixels);
		texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		labelSeconds = new HudLabel(scale, FontFace.Roboto, "s", false, 0.5f);
		labelSeconds.setAlpha(0);

		//
		texProgress = Art.texCircleProgressHalf;
		texMask = Art.texCircleProgressHalfMask;

		w = texProgress.getWidth();
		h = texProgress.getHeight();
		offX = w / 2;
		offY = h / 2;
		shProgress = ShaderLoader.fromFile("progress", "progress");
		sProgress = new Sprite(texProgress);
		sProgress.flip(false, true);
	}

	public void setSeconds (float seconds) {
		this.seconds = MathUtils.clamp(seconds, 0, MaxSeconds);
	}

	public float getSeconds () {
		return seconds;
	}

	public void showSecondsLabel () {
		labelSeconds.queueShow(300);
	}

	public void hideSecondsLabel () {
		labelSeconds.queueHide(300);
	}

	@Override
	public void dispose () {
		texture.dispose();
		pixels.dispose();
	}

	public void tick () {
		labelSeconds.tick();
	}

	public void render (SpriteBatch batch, float cameraZoom) {
// pixels.setColor(0.25f, 0.25f, 0.25f, 0.5f);
// pixels.fill();
//
// float ratio = seconds / MaxSeconds;
//
// int w = MathUtils.clamp((int)(width * ratio) - 2, 0, (int)width);
//
// if (w > 0) {
// // full = 173|255|30 / 0.678 | 1 | 0.118
// // mid = 247|194|29 / 0.969 | 0.761 | 0.114
// // low = 247|30|29 / 0.969 | 0.118 | 0.114
//
// float greenRatio = MathUtils.clamp(ratio, 0.23f, 1);
// float rbRange = (1 - MathUtils.clamp(greenRatio, 0.761f, 1)) / (1 - 0.761f);
//
// barColor.r = 0.678f + (0.969f - 0.678f) * rbRange;
// barColor.g = greenRatio;
// barColor.b = 0.118f - (0.118f - 0.114f) * rbRange;
//
// pixels.setColor(barColor);
// pixels.fillRectangle(1, 1, w, (int)height - 2);
// }
//
// texture.draw(pixels, 0, 0);
//
// float x = position.x - halfWidth * cameraZoom;
// float y = position.y - halfHeight * cameraZoom;
// float ww = (width * cameraZoom);
// float hh = (height * cameraZoom);
// batch.draw(texture, x, y, ww, hh);

		labelSeconds.setScale(0.5f * cameraZoom, false);
		labelSeconds.setString(NumberString.format(seconds) + "s", true);
		labelSeconds.setPosition(position.x, position.y + Convert.scaledPixels(80) * cameraZoom * scale);
		labelSeconds.render(batch);

		// circle progress for remaining time
		float s = 0.8f;
		float scl = cameraZoom * scale * s;
		float px = position.x - offX;
		float py = position.y - offY + Convert.scaledPixels(32) * cameraZoom * s;
		float a = 1f;

		batch.setShader(shProgress);
		texMask.bind(1);
		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0);
		shProgress.setUniformi("u_texture1", 1);

		float ratio = seconds / MaxSeconds;
		shProgress.setUniformf("progress", ratio);
		sProgress.setColor(ColorUtils.paletteRYG(ratio, 1));
		sProgress.setScale(scl);
		sProgress.setPosition(px, py);
		sProgress.draw(batch, a);
		batch.flush();

		batch.setShader(null);
	}
}
