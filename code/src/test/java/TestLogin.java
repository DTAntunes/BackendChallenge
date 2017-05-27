package testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.restfb.DefaultJsonMapper;
import com.restfb.FacebookClient;
import com.restfb.JsonMapper;
import com.restfb.Parameter;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchRequest.BatchRequestBuilder;
import com.restfb.batch.BatchResponse;
import com.restfb.types.TestUser;

import login.UserController;
import login.UserModel;
import login.responseModels.CreateResponse;
import util.Configuration;
import util.Paths;
import util.StatusCodes;

public class TestLogin {

	private static class User {

		public final String accessToken, userId;

		public User(String accessToken, String userId) {
			this.accessToken = accessToken;
			this.userId = userId;
		}
	}

	private static final String LOGIN_URL = TestUtility.BASE_URL + Paths.LOGIN;
	private static final Gson DESERIALISER = new Gson();

	private static User allScopes;

	private static User missingFriends;
	private static HttpClient client;

	private static void confirmOkResponse(ContentResponse res, String userId) {
		CreateResponse info = DESERIALISER.fromJson(new String(res.getContent()),
		                                            CreateResponse.class);
		assertEquals("No denied permissions should be returned with OK", info.data.length, 0);
		assertEquals("User token returned should match user's",
		             ((LinkedTreeMap) info.meta.get("accessInfo")).get("userId"), userId);
		assertNotNull("Long term access token should exist under user's ID",
		              UserModel.getUser(userId));
	}

	@BeforeClass
	public static void setUpServer() throws Exception {
		System.out.println("\nThis test is going to take a while (~30s) - creating and deleting test users is slow.\n");
		TestUtility.startServer();
		FacebookClient fbClient = Configuration.FB_CLIENT;

		String permissions = UserController.PLACES_SCOPE + "," + UserController.LIKES_SCOPE + ","
		                     + UserController.FRIENDS_SCOPE;
		String testUserPath = Configuration.FB_APP_ID + "/accounts/test-users";
		Parameter[] parameters = new Parameter[] { Parameter.with("installed", "true"),
		                                           Parameter.with("permissions", permissions) };
		BatchRequest noFriends = new BatchRequestBuilder(testUserPath).method("POST")
		                                                              .parameters(parameters)
		                                                              .name("first").build();
		BatchRequest allUser = new BatchRequestBuilder(testUserPath).method("POST")
		                                                            .parameters(parameters).build();

		JsonMapper mapper = new DefaultJsonMapper();
		List<BatchResponse> response = fbClient.executeBatch(noFriends, allUser);
		BatchResponse noFriendResponse = response.get(0);
		if (noFriendResponse.getCode() != 200) {
			fail("Couldn't create no friend test user");
		} else {
			TestUser user = mapper.toJavaObject(noFriendResponse.getBody(), TestUser.class);
			missingFriends = new User(user.getAccessToken(), user.getId());
			// this didn't want to work with the batch request for some reason
			fbClient.deleteObject(user.getId() + "/permissions/" + UserController.FRIENDS_SCOPE);
		}

		BatchResponse allScopeResponse = response.get(1);
		if (allScopeResponse.getCode() != 200) {
			fail("Couldn't create all scopes test user");
		} else {
			TestUser user = mapper.toJavaObject(allScopeResponse.getBody(), TestUser.class);
			allScopes = new User(user.getAccessToken(), user.getId());
		}

		client = new HttpClient();
		client.start();
	}

	@AfterClass
	public static void tearDown() {
		BatchRequest deleteFriends = new BatchRequestBuilder(missingFriends.userId).method("DELETE")
		                                                                           .build();
		BatchRequest deleteAll = new BatchRequestBuilder(allScopes.userId).method("DELETE").build();
		Configuration.FB_CLIENT.executeBatch(deleteAll, deleteFriends);
	}

	@Test
	public void testLoginFailures() {
		try {
			ContentResponse res = client.POST(LOGIN_URL).send();
			assertEquals("Checking that no token submitted leads to auth error",
			             StatusCodes.ClientError.UNAUTHORIZED, res.getStatus());

			String withTokenPart = LOGIN_URL + "?accessToken=";
			res = client.POST(withTokenPart
			                  + TestUtility.invalidateAccessToken(allScopes.accessToken))
			            .send();
			assertEquals("Checking that invalid access token leads to auth error",
			             StatusCodes.ClientError.UNAUTHORIZED, res.getStatus());

			res = client.POST(withTokenPart + "CeciNestPasUnAccessToken").send();
			assertEquals("Checking that arbitrary access token leads to auth error",
			             StatusCodes.ClientError.UNAUTHORIZED, res.getStatus());
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail("See exception.");
		}
	}

	@Test
	public void testLoginSuccesses() {
		try {
			String withTokenParam = LOGIN_URL + "?accessToken=";
			ContentResponse res = client.POST(withTokenParam + allScopes.accessToken).send();
			assertEquals("Successful login with no denied permissions leads to OK",
			             StatusCodes.Success.OK, res.getStatus());
			confirmOkResponse(res, allScopes.userId);

			String noFriendsLogin = withTokenParam + missingFriends.accessToken;
			res = client.POST(noFriendsLogin).send();
			assertEquals("Successful login missing a scope leads to Accepted",
			             StatusCodes.Success.ACCEPTED, res.getStatus());

			CreateResponse info = DESERIALISER.fromJson(new String(res.getContent()),
			                                            CreateResponse.class);
			assertEquals("Should be missing one permission for friends", 1, info.data.length);
			assertEquals("Missing scope should be friends", UserController.FRIENDS_SCOPE,
			             info.data[0].id);
			assertEquals("Missing scope type is permission", info.data[0].type, "permission");
			assertEquals("User token returned should match user's", missingFriends.userId,
			             ((LinkedTreeMap) info.meta.get("accessInfo")).get("userId"));
			assertNotNull("Long term access token should exist under user's ID",
			              UserModel.getUser(missingFriends.userId));

			res = client.POST(noFriendsLogin).send();
			assertEquals("Repeat login with missing permission should give OK",
			             StatusCodes.Success.OK, res.getStatus());
			confirmOkResponse(res, missingFriends.userId);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
			fail("See exception.");
		}
	}
}
