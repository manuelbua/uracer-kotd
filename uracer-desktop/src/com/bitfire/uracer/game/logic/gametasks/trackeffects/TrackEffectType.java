
package com.bitfire.uracer.game.logic.gametasks.trackeffects;

/** Defines the type of special effect, it also describes their rendering order (FIXME) */
public enum TrackEffectType {
	// @off
	CarSkidMarks(1),
	CarSmokeTrails(2);
	// @on

	public final int id;

	private TrackEffectType (int id) {
		this.id = id;
	}

}
