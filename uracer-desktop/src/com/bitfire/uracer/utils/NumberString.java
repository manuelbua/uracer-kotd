package com.bitfire.uracer.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class NumberString {
	private static NumberFormat formatter = null;
	private static NumberFormat formatterLong = null;

	private NumberString() {
	}

	public static String format( float value ) {
		if( NumberString.formatter == null ) {
			NumberString.formatter = new DecimalFormat( "#.###" );
		}

		return NumberString.formatter.format( (double)value );
	}

	public static String formatLong( float value ) {
		if( NumberString.formatterLong == null ) {
			NumberString.formatterLong = new DecimalFormat( "#.######" );
		}

		return NumberString.formatterLong.format( (double)value );
	}

}
