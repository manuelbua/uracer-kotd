
package com.bitfire.uracer.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class SortedProperties extends Properties {
	private static final long serialVersionUID = 2029498762824558018L;

	/** Overrides, called by the store method. */
	@Override
	@SuppressWarnings("unchecked")
	public synchronized Enumeration keys () {
		Enumeration keysEnum = super.keys();
		List<String> keys = new ArrayList<String>();
		while (keysEnum.hasMoreElements()) {
			keys.add((String)keysEnum.nextElement());
		}
		Collections.sort(keys, new Comparator<String>() {

			@Override
			public int compare (String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}

		});
		return Collections.enumeration(keys);
	}
}
