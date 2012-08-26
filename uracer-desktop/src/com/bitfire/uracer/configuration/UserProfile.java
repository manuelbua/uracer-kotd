
package com.bitfire.uracer.configuration;

public final class UserProfile {

	public enum Nation {
		None
	}

	public final long userId;
	public final String userName;
	public final String userNationCode;

	public UserProfile () {
		userId = -1;
		userName = "Manuel";
		userNationCode = "it";
	}
}
