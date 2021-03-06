package util;

import java.io.IOException;
import java.util.Properties;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultWebRequestor;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.WebRequestor;

import spark.Spark;
import spoofing.MusicSpoofWebRequestor;

/**
 * This allows easy swapping of endpoints and configurations etc for testing.
 *
 * @author Dário T A
 */
public class Configuration {

	public static final Version FB_API_VERSION = Version.VERSION_2_9;
	public static final String FB_APP_ID = "1005335112935553";
	public static final String APP_ACCESS_TOKEN, APP_SECRET;
	public static final AmazonDynamoDB DB_CLIENT;
	public static final FacebookClient FB_CLIENT;
	public static final boolean TESTING;
	public static final WebRequestor WEB_REQUESTOR;

	static {
		// Retrieve the secret properties
		Properties properties = new Properties();
		ClassLoader classLoader = Configuration.class.getClassLoader();
		try {
			properties.load(classLoader.getResourceAsStream("config.properties"));
		} catch (IOException ex) {
			ex.printStackTrace();
			System.err.println("Couldn't initialise correctly, exiting.");
			System.exit(1);
		}

		EndpointConfiguration dynamoEndpoint;
		AWSCredentialsProvider credentialsProvider;
		// Set up things as needed for testing/not
		if (TESTING = Boolean.parseBoolean(properties.getProperty("testing"))) {
			dynamoEndpoint = new EndpointConfiguration("http://localhost:8765",
			                                           Regions.EU_WEST_1.getName());
			credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("blah",
			                                                                               "blah"));

			// this lets us spoof music endpoint responses for testing
			WEB_REQUESTOR = new MusicSpoofWebRequestor();

			// Spark exception handler to simply print exceptions while testing
			Spark.exception(Exception.class, (exception, request, response) -> {
				exception.printStackTrace();
				response.status(500);
				response.body("Internal Server Error, check console.");
			});
		} else {
			// Otherwise let's just assume EU_WEST_1 for now
			Regions liveRegion = Regions.EU_WEST_1;
			dynamoEndpoint = new EndpointConfiguration(Region.getRegion(liveRegion)
			                                                 .getServiceEndpoint(AmazonDynamoDB.ENDPOINT_PREFIX),
			                                           liveRegion.getName());
			credentialsProvider = new DefaultAWSCredentialsProviderChain();

			WEB_REQUESTOR = new DefaultWebRequestor();
		}
		DB_CLIENT = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(dynamoEndpoint)
		                                       .withCredentials(credentialsProvider).build();

		APP_SECRET = properties.getProperty("appSecret");
		// This isn't the ideal way to generate an access token - there's a
		// proper API call. However, that seemed to generate tokens that only
		// worked in Graph Explorer, possibly because the application's not
		// accepted.
		APP_ACCESS_TOKEN = FB_APP_ID + "|" + APP_SECRET;

		FB_CLIENT = new DefaultFacebookClient(Configuration.APP_ACCESS_TOKEN,
		                                      Configuration.FB_API_VERSION);
	}

}
