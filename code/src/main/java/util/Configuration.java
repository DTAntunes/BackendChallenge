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
import com.restfb.Version;

import spark.Spark;

/**
 * This allows easy swapping of endpoints and configurations etc for testing.
 *
 * @author Dário T A
 */
public class Configuration {

	public static final Version FB_API_VERSION = Version.VERSION_2_9;
	public static final EndpointConfiguration DYNAMO_ENDPOINT;
	public static final AWSCredentialsProvider CREDENTIALS_PROVIDER;
	public static final String FB_APP_ID = "1005335112935553";
	public static final String APP_ACCESS_TOKEN;

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

		// Set up things as needed for testing/not
		if (Boolean.parseBoolean(properties.getProperty("testing"))) {
			// According to AWS docs, US_WEST_2 is the signing region to use
			// for local testing - I haven't checked if it actually matters
			DYNAMO_ENDPOINT = new EndpointConfiguration("http://localhost:8765",
			                                            Regions.US_WEST_2.getName());
			CREDENTIALS_PROVIDER = new AWSStaticCredentialsProvider(new BasicAWSCredentials("blah",
			                                                                                "blah"));

			// Spark exception handler to simply print exceptions while testing
			Spark.exception(Exception.class, (exception, request, response) -> {
				exception.printStackTrace();
			});
		} else {
			// Otherwise let's just assume EU_WEST_1 for now
			Regions liveRegion = Regions.EU_WEST_1;
			DYNAMO_ENDPOINT = new EndpointConfiguration(Region.getRegion(liveRegion)
			                                                  .getServiceEndpoint(AmazonDynamoDB.ENDPOINT_PREFIX),
			                                            liveRegion.getName());
			CREDENTIALS_PROVIDER = new DefaultAWSCredentialsProviderChain();
		}

		// This isn't the ideal way to generate an access token - there's a
		// proper API call. However, that seemed to generate tokens that only
		// worked in Graph Explorer, possibly because the application's not
		// accepted.
		APP_ACCESS_TOKEN = FB_APP_ID + "|" + properties.getProperty("appSecret");
	}

}
