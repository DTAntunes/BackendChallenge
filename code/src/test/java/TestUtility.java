package testing;

import base.GonnaTrackYou;

public class TestUtility {
	
	private static final String PROTOCOL_AND_HOST = "http://localhost";
	public static final int TEST_SPARK_PORT = GonnaTrackYou.SPARK_PORT + 1;
	public static final String BASE_URL = PROTOCOL_AND_HOST + ':' + TEST_SPARK_PORT;
	
	public static final String invalidateAccessToken(String token) {
		if (token.toUpperCase().equals(token)) {
			return token.toLowerCase();
		} else if (token.toLowerCase().equals(token)) {
			return token.toUpperCase();
		} else {
			return token.toUpperCase();
		}
	}

}
