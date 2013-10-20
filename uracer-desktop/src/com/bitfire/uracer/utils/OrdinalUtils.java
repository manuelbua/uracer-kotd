
package com.bitfire.uracer.utils;

public class OrdinalUtils {
	public static String getOrdinalFor (int value) {
		int hundredRemainder = value % 100;
		int tenRemainder = value % 10;
		if (hundredRemainder - tenRemainder == 10) {
			return "th";
		}

		switch (tenRemainder) {
		case 1:
			return "st";
		case 2:
			return "nd";
		case 3:
			return "rd";
		default:
			return "th";
		}
	}

	private OrdinalUtils () {
	}
}
