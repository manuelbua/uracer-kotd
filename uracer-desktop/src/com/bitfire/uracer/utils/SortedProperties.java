
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
	public synchronized Enumeration<Object> keys () {
		Enumeration<Object> keysEnum = super.keys();
		List<Object> keys = new ArrayList<Object>();
		while (keysEnum.hasMoreElements()) {
			keys.add((String)keysEnum.nextElement());
		}
		Collections.sort(keys, new Comparator<Object>() {

			@Override
			public int compare (Object o1, Object o2) {
				String s1 = (String)o1;
				String s2 = (String)o2;
				return s1.compareToIgnoreCase(s2);
			}

		});
		return Collections.enumeration(keys);
	}
}
