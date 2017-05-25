package login;

import static util.JsonRenderer.render;

import java.util.HashSet;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient.DebugTokenInfo;

import login.responseModels.CreateResponse;
import spark.Request;
import spark.Route;
import util.Configuration;
import util.StatusCodes;

public class UserController {

	private static final String TOKEN_NAME = "accessToken";
	private static final String USER_ID_NAME = "userId";
	private static final HashSet<String> REQUIRED_SCOPES = new HashSet<>();
	static {
		REQUIRED_SCOPES.add("user_likes");
		REQUIRED_SCOPES.add("user_friends");
		REQUIRED_SCOPES.add("user_tagged_places");
	}

	public static final Route CREATE_USER = (request, response) -> {
		DefaultFacebookClient fbClient = new DefaultFacebookClient(Configuration.APP_ACCESS_TOKEN,
		                                                           Configuration.FB_API_VERSION);

		String accessToken = request.queryParams(TOKEN_NAME);
		DebugTokenInfo info = fbClient.debugToken(accessToken);

		// Unauthorised response for an invalid token, otherwise check if they
		// have an account with us already
		if (!info.isValid()) {
			response.status(StatusCodes.ClientError.UNAUTHORIZED);
			return render(null);
		} else {
			UserModel user = new UserModel(info.getUserId(), accessToken, info.getScopes());

			request.session().attribute(TOKEN_NAME, user.getAccessToken());
			request.session().attribute(USER_ID_NAME, user.getUserId());

			@SuppressWarnings("unchecked")
			HashSet<String> deniedPermissions = (HashSet<String>) REQUIRED_SCOPES.clone();
			deniedPermissions.removeAll(REQUIRED_SCOPES);

			// update the access token
			user.updateItem();

			// check which of the 200s we should be returning here
			if (deniedPermissions.isEmpty()) {
				response.status(StatusCodes.Success.OK);
				return render(new CreateResponse(user));
			} else {
				// They didn't accept everything
				response.status(StatusCodes.Success.ACCEPTED);
				return render(new CreateResponse(deniedPermissions, user));
			}
		}
	};

}
