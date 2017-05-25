package login;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient.DebugTokenInfo;

import spark.Route;
import util.Configuration;
import util.StatusCodes;

public class UserController {

	public static final Route CREATE_USER = (request, response) -> {
		String accessToken = request.queryParams("accessToken"), result = "";
		DefaultFacebookClient fbClient = new DefaultFacebookClient(Configuration.APP_ACCESS_TOKEN,
		                                                           Configuration.FB_API_VERSION);
		DebugTokenInfo info = fbClient.debugToken(accessToken);

		// Unauthorised response for an invalid token, otherwise check if they
		// have an account with us already
		if (!info.isValid()) {
			System.out.println(401);
			response.status(StatusCodes.ClientError.UNAUTHORIZED);
		} else {
			UserModel user = new UserModel(info.getUserId(), accessToken);

			ArrayList<PermissionGrant> deniedGrants = user.getDeniedPermissions(fbClient);

			// check which of the 200s we should be returning here
			if (deniedGrants.isEmpty()) {
				if (user.exists()) {
					System.out.println(204);
					response.status(StatusCodes.Success.NO_CONTENT);
				} else {
					System.out.println(201);
					response.status(StatusCodes.Success.CREATED);
				}
			} else {
				// They didn't accept everything
				System.out.println(202);
				response.status(StatusCodes.Success.ACCEPTED);
				result = new Gson().toJson(deniedGrants);
			}

			// update the access token
			user.updateItem();
		}

		return result;
	};

}
