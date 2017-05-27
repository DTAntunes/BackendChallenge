package login;

import static util.Configuration.FB_CLIENT;
import static util.JsonRenderer.render;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultJsonMapper;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.FacebookClient.DebugTokenInfo;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.json.JsonObject;
import com.restfb.types.PlaceTag;
import com.restfb.types.User;

import login.responseModels.CreateResponse;
import login.responseModels.LocationPreference;
import login.responseModels.MusicPreference;
import login.responseModels.Popularity;
import spark.Request;
import spark.Route;
import util.Configuration;
import util.ResourceObject;
import util.StatusCodes;

public class UserController {

	public static final String LOGIN_TOKEN_NAME = "token", USER_ID_NAME = "userId";;
	private static final String ACCESS_TOKEN_NAME = "accessToken";
	private static final SecureRandom ENTROPY_SOURCE = new SecureRandom();
	private static final int TOKEN_BYTES = 64;
	public static final String LIKES_SCOPE = "user_likes", FRIENDS_SCOPE = "user_friends",
	        PLACES_SCOPE = "user_tagged_places";
	private static final HashSet<String> REQUIRED_SCOPES = new HashSet<>();
	static {
		REQUIRED_SCOPES.add(LIKES_SCOPE);
		REQUIRED_SCOPES.add(FRIENDS_SCOPE);
		REQUIRED_SCOPES.add(PLACES_SCOPE);
	}

	public static final Route LOGIN = (request, response) -> {
		String shortToken = request.queryParams(ACCESS_TOKEN_NAME);

		if (shortToken == null) {
			response.status(401);
			return render(null);
		}

		DebugTokenInfo info;
		try {
			info = FB_CLIENT.debugToken(shortToken);
		} catch (FacebookOAuthException e) {
			// This can happen, for example, with an access token intended for
			// another app
			if (Configuration.TESTING) {
				throw e;
			} else {
				response.status(401);
				return render(null);
			}
		}

		// Unauthorised response for an invalid token, otherwise check if they
		// have an account with us already
		if (!info.isValid()) {
			response.status(StatusCodes.ClientError.UNAUTHORIZED);
			return render(null);
		} else {
			AccessToken longToken = FB_CLIENT.obtainExtendedAccessToken(Configuration.FB_APP_ID,
			                                                            Configuration.APP_SECRET,
			                                                            shortToken);
			UserModel user = new UserModel(info.getUserId(), longToken.getAccessToken(),
			                               info.getScopes());
			LoginModel login = user.getLoginData(generateToken());

			request.session().attribute(LOGIN_TOKEN_NAME, login.token);
			request.session().attribute(USER_ID_NAME, login.userId);

			@SuppressWarnings("unchecked")
			HashSet<String> deniedPermissions = (HashSet<String>) REQUIRED_SCOPES.clone();
			deniedPermissions.removeAll(info.getScopes());

			// store the login token for future request validation
			login.putItem();

			CreateResponse result;
			// check which of the 200s we should be returning here as FB says
			// you should only re-request denied permissions once
			if (user.exists() || deniedPermissions.isEmpty()) {
				response.status(StatusCodes.Success.OK);
				result = new CreateResponse(login);
			} else {
				// They didn't accept everything on first login
				response.status(StatusCodes.Success.ACCEPTED);
				result = new CreateResponse(deniedPermissions, login);
			}

			// update the access token and scopes
			user.putItem();
			return render(result);
		}
	};

	public static final Route GET_DATA = (request, response) -> {
		LoginModel login = extractAccessDetails(request);

		if (login.isValid()) {
			UserModel user = UserModel.getUser(login.userId);

			if (user == null || !user.validAccessToken()) {
				response.status(StatusCodes.ClientError.FORBIDDEN);
				return render(null);
			}

			Map<String, Object> userData = new HashMap<>();
			FacebookClient fbClient = new DefaultFacebookClient(user.getAccessToken(),
			                                                    Configuration.WEB_REQUESTOR,
			                                                    new DefaultJsonMapper(),
			                                                    Configuration.FB_API_VERSION);
			boolean anyData = false;

			Popularity popularity;
			if (user.hasScope(FRIENDS_SCOPE)) {
				anyData = true;
				Connection<User> fbUsers = fbClient.fetchConnection("me/friends", User.class);
				popularity = new Popularity(fbUsers.getTotalCount(), false);
			} else {
				popularity = new Popularity(null, true);
			}
			userData.put("popular", popularity);

			MusicPreference music;
			if (user.hasScope(LIKES_SCOPE)) {
				anyData = true;
				music = UserDataUtility.getPreferredBand(fbClient.fetchConnection("me/music",
				                                                                  JsonObject.class)
				                                                 .iterator());
			} else {
				music = new MusicPreference(null, null, true);
			}
			userData.put("favouriteMusic", music);

			LocationPreference location;
			if (user.hasScope(PLACES_SCOPE)) {
				anyData = true;
				location = UserDataUtility.getPreferredLocation(fbClient.fetchConnection("me/tagged_places",
				                                                                         PlaceTag.class)
				                                                        .iterator());
			} else {
				location = new LocationPreference(null, null, true);
			}
			userData.put("favouritePlace", location);

			if (!anyData) {
				response.status(StatusCodes.ClientError.NOT_FOUND);
				return render(null);
			}

			return render(wrap(new ResourceObject(login.userId, "interests", userData)));
		} else {
			response.status(StatusCodes.ClientError.UNAUTHORIZED);
			return render(null);
		}
	};

	private static LoginModel extractAccessDetails(Request request) {
		String token = getRedundantParameter(request, LOGIN_TOKEN_NAME),
		        userId = getRedundantParameter(request, USER_ID_NAME);

		return new LoginModel(userId, token);
	}

	private static String generateToken() {
		byte[] token = new byte[TOKEN_BYTES];
		ENTROPY_SOURCE.nextBytes(token);
		return Base64.getEncoder().encodeToString(token);
	}

	private static final String getRedundantParameter(Request request, String name) {
		String param = request.session().attribute(name);
		param = param == null ? request.queryParams(name) : param;
		return param;
	}

	private static Map<String, Object> wrap(ResourceObject res) {
		HashMap<String, Object> map = new HashMap<>();
		map.put("data", res);
		return map;
	}
}
