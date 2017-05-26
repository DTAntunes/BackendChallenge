package testing;

import base.GonnaTrackYou;

public class TestConfig {
	
	private static final String PROTOCOL_AND_HOST = "http://localhost";
	public static final int TEST_SPARK_PORT = GonnaTrackYou.SPARK_PORT + 1;
	public static final String BASE_URL = PROTOCOL_AND_HOST + ':' + TEST_SPARK_PORT;

}
