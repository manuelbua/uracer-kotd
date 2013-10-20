
package com.bitfire.uracer.configuration;

public final class UserProfile {

	public enum Nation {
		None
	}

	public final String userId;
	public final String userName;
	public final String userCountryCode;

	public UserProfile () {
		userId = "uid1";
		userName = "Manuel";
		userCountryCode = "it";
	}
}
