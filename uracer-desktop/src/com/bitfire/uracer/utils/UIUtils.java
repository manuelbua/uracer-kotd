package com.bitfire.uracer.utils;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.bitfire.uracer.resources.Art;

public final class UIUtils {

	public static TextButton newTextButton( String text, ClickListener listener ) {
		TextButton btn = new TextButton( text, Art.scrSkin );
		btn.setClickListener( listener );
		return btn;
	}

	public static CheckBox newCheckBox( String text, int width, boolean checked ) {
		CheckBox cb = new CheckBox( text, Art.scrSkin );
		cb.width( width );
		cb.getLabelCell().pad( 5 );
		cb.setChecked( checked );
		return cb;
	}

	private UIUtils() {
	}
}
