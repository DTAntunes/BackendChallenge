package testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.restfb.ConnectionIterator;
import com.restfb.types.TestUser;

import base.GonnaTrackYou;
import login.LoginModel;
import login.responseModels.LocationPreference;
import login.responseModels.MusicPreference;
import login.responseModels.Popularity;
import spoofing.MusicSpoofWebRequestor;
import util.Configuration;
import util.Paths;
import util.ResourceObject;
import util.StatusCodes;

public class UserDataTests {

	private static HashMap<String, ResourceObject> responses = new HashMap<>();
	private static HttpClient client;
	private static Gson deserialiser = new Gson();

	private static void checkLocation(String userId, LocationPreference expected,
	                                  LinkedTreeMap actual) {
		// These tests may fail while Facebook tags aren't yet consistent (up to 12 hours for
		// tagged_places)
		assertEquals("Checking place for user " + userId, expected.place, actual.get("place"));
		if (expected.count == null) {
			assertNull("Checking place visit count for user " + userId, actual.get("count"));
		} else {
			assertEquals("Checking place visit count for user " + userId, expected.count.intValue(),
			             ((Double) actual.get("count")).intValue());
		}
		assertEquals("Checking place error for user " + userId, expected.error,
		             actual.get("error"));
	}

	private static void checkMusic(String userId, MusicPreference expectedMusic,
	                               LinkedTreeMap actualMusic) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZ");

		assertEquals("Checking band for user " + userId, expectedMusic.band,
		             actualMusic.get("band"));
		if (expectedMusic.likeDate == null) {
			assertNull("Checking band like date for user " + userId, actualMusic.get("likeDate"));
		} else {
			assertEquals("Checking band like date for user " + userId,
			             dateFormat.parse(expectedMusic.likeDate),
			             dateFormat.parse((String) actualMusic.get("likeDate")));
		}
		assertEquals("Checking music error for user " + userId, expectedMusic.error,
		             actualMusic.get("error"));
	}

	private static void checkPopularity(String userId, Popularity expected, LinkedTreeMap actual) {
		assertEquals("Checking popularity for user " + userId, expected.popular,
		             actual.get("popular"));
		assertEquals("Checking popularity measure for user " + userId,
		             expected.popularMeasure.intValue(),
		             ((Double) actual.get("popularMeasure")).intValue());
		assertEquals("Checking popularity error for user " + userId, expected.error,
		             actual.get("error"));
	}

	@BeforeClass
	public static void setUp() throws Exception {
		GonnaTrackYou.startServer(TestConfig.TEST_SPARK_PORT);

		// these test users are set up manually beforehand
		ClassLoader loader = MusicSpoofWebRequestor.class.getClassLoader();
		Scanner scan = new Scanner(loader.getResourceAsStream("userDataResponses"));

		while (scan.hasNextLine()) {
			HashMap<String, String> results = new HashMap<>();
			String userId = scan.nextLine();
			results.put("popular", scan.nextLine());
			results.put("favouritePlace", scan.nextLine());
			results.put("favouriteMusic", scan.nextLine());
			responses.put(userId, new ResourceObject(userId, "interests",
			                                         (HashMap<String, Object>) results.clone()));
		}

		scan.close();

		client = new HttpClient();
		client.start();
	}

	public void checkUser(TestUser user) throws ParseException {
		ContentResponse res = null;
		try {
			// Retrieve the access token and create an account/log in
			res = client.POST(TestConfig.BASE_URL + Paths.LOGIN + "?accessToken="
			                  + user.getAccessToken())
			            .send();
			LoginModel login = deserialiser.fromJson(new String(res.getContent()),
			                                         LoginModel.class);

			// Retrieve user data
			res = client.GET(TestConfig.BASE_URL + Paths.GET_DATA + "?token=" + login.getToken()
			                 + "&userId=" + user.getId());
		} catch (InterruptedException | TimeoutException | ExecutionException e) {
			e.printStackTrace();
			fail("See exception");
		}

		assertEquals("Checking status code for " + user.getId(), StatusCodes.Success.OK,
		             res.getStatus());

		String content = res.getContentAsString();

		// strip the data key wrapper and get the interesting part of the response
		HashMap<String,
		        Object> response = deserialiser.fromJson(content.substring("{\"data\":".length(),
		                                                                   content.length() - 1),
		                                                 HashMap.class);

		// extract our expected responses
		ResourceObject expectedResponse = responses.get(user.getId());
		Map<String, Object> attributes = expectedResponse.attributes;
		MusicPreference expectedMusic = deserialiser.fromJson((String) attributes.get("favouriteMusic"),
		                                                      MusicPreference.class);
		LocationPreference expectedPlace = deserialiser.fromJson((String) attributes.get("favouritePlace"),
		                                                         LocationPreference.class);
		Popularity expectedPopularity = deserialiser.fromJson((String) attributes.get("popular"),
		                                                      Popularity.class);

		// check the JSON API required keys
		assertEquals(expectedResponse.id, response.get("id"));
		assertEquals(expectedResponse.type, response.get("type"));

		// check the individual responses
		LinkedTreeMap actualAttributes = (LinkedTreeMap) response.get("attributes");

		LinkedTreeMap actualMusic = (LinkedTreeMap) actualAttributes.get("favouriteMusic");
		checkMusic(user.getId(), expectedMusic, actualMusic);

		LinkedTreeMap actualPopularity = (LinkedTreeMap) actualAttributes.get("popular");
		checkPopularity(user.getId(), expectedPopularity, actualPopularity);

		LinkedTreeMap actualPlace = (LinkedTreeMap) actualAttributes.get("favouritePlace");
		checkLocation(user.getId(), expectedPlace, actualPlace);
	}

	@Test
	public void testFailUserData() {
		String userId = "10017585221011";
		ConnectionIterator<TestUser> users = Configuration.FB_CLIENT.fetchConnection(Configuration.FB_APP_ID
		                                                                             + "/accounts/test-users",
		                                                                             TestUser.class)
		                                                            .iterator();
	}

	@Test
	public void testRetrieveUserData() {
		// It seems there's no way to retrieve a token for a specific user - right now it's simpler
		// to paginate through looking for them all but it would be better in future to store long
		// lived tokens in a config file or similar (to populate the DB)
		ConnectionIterator<TestUser> users = Configuration.FB_CLIENT.fetchConnection(Configuration.FB_APP_ID
		                                                                             + "/accounts/test-users",
		                                                                             TestUser.class)
		                                                            .iterator();

		int remaining = responses.size();
		while (users.hasNext() && remaining > 0) {
			for (TestUser user : users.next()) {
				if (responses.containsKey(user.getId())) {
					remaining--;

					try {
						checkUser(user);
					} catch (ParseException e) {
						e.printStackTrace();
						fail("See exception");
					}

					if (remaining == 0) {
						break;
					}
				}
			}
		}

		if (remaining > 0) {
			fail("Not all users handled");
		}
	}

}
