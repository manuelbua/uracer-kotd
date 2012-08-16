
package com.bitfire.uracer.screen;

public interface ScreenFactory {
	public interface ScreenId {
		public abstract int id ();
	}

	Screen createScreen (ScreenId screenId);
}
