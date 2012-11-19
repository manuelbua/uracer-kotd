
package com.bitfire.uracer.utils;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bitfire.uracer.resources.Art;

public final class UIUtils {

	public static TextButton newTextButton (String text, ClickListener listener) {
		TextButton btn = new TextButton(text, Art.scrSkin);
		btn.addListener(listener);
		return btn;
	}

	public static CheckBox newCheckBox (String text, boolean checked) {
		CheckBox cb = new CheckBox(text, Art.scrSkin);
		cb.setChecked(checked);
		return cb;
	}

	public static SelectBox newSelectBox (Object[] items) {
		return newSelectBox(items, null);
	}

	public static SelectBox newSelectBox (Object[] items, ChangeListener listener) {
		SelectBox sb = new SelectBox(items, Art.scrSkin);
		if (listener != null) {
			sb.addListener(listener);
		}
		return sb;
	}

	private UIUtils () {
	}
}
