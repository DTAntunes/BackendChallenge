package login;

import static util.JsonRenderer.render;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.FacebookClient.DebugTokenInfo;

import login.responseModels.CreateResponse;
import spark.Request;
import spark.Route;
import util.Configuration;
import util.StatusCodes;

public class UserController {

	private static final String LOGIN_TOKEN_NAME = "token";
	private static final String ACCESS_TOKEN_NAME = "accessToken";
	private static final String USER_ID_NAME = "userId";
	private static final SecureRandom ENTROPY_SOURCE = new SecureRandom();
	private static final int TOKEN_BYTES = 64;
	private static final HashSet<String> REQUIRED_SCOPES = new HashSet<>();
	static {
		REQUIRED_SCOPES.add("user_likes");
		REQUIRED_SCOPES.add("user_friends");
		REQUIRED_SCOPES.add("user_tagged_places");
	}

	public static final Route LOGIN = (request, response) -> {
		DefaultFacebookClient fbClient = new DefaultFacebookClient(Configuration.APP_ACCESS_TOKEN,
		                                                           Configuration.FB_API_VERSION);
		String shortToken = request.queryParams(ACCESS_TOKEN_NAME);
		DebugTokenInfo info = fbClient.debugToken(shortToken);

		// Unauthorised response for an invalid token, otherwise check if they
		// have an account with us already
		if (!info.isValid()) {
			response.status(StatusCodes.ClientError.UNAUTHORIZED);
			return render(null);
		} else {
			AccessToken longToken = fbClient.obtainExtendedAccessToken(Configuration.FB_APP_ID,
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

			// update the access token and scopes
			user.putItem();
			// store the login token for future request validation
			login.putItem();

			// check which of the 200s we should be returning here as FB says
			// you should only re-request denied permissions once
			if (user.exists()) {
				response.status(StatusCodes.Success.OK);
				return render(new CreateResponse(login));
			} else {
				// They didn't accept everything on first login
				response.status(StatusCodes.Success.ACCEPTED);
				return render(new CreateResponse(deniedPermissions, login));
			}
		}
	};

	private static String generateToken() {
		byte[] token = new byte[TOKEN_BYTES];
		ENTROPY_SOURCE.nextBytes(token);
		return Base64.getEncoder().encodeToString(token);
	}
}
