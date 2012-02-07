package com.bitfire.uracer.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class FloatString
{
	private static NumberFormat formatter = null;
	public static String format( float value )
	{
		if( FloatString.formatter == null )
		{
			FloatString.formatter = new DecimalFormat("#.###");
		}

		return FloatString.formatter.format( (double )value );
	}

}
