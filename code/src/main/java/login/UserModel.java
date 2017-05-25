package login;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.restfb.FacebookClient;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonObject;

import util.Configuration;

/**
 * Models a user by user ID and most recently used access token. Does not track
 * denied permissions - I'm assuming FB responses will cover that suitably so I
 * don't need to complicate things by tracking it separately. This has a
 * performance penalty, but I'll ignore it for now.
 *
 * @author Dário T A
 */
public class UserModel {

	public static final String TABLE_NAME = "users", USER_ID = "userId", ACCESS_TOKEN = "token";

	private String userId, accessToken;
	// TODO check if this could/should be a singleton
	private Table table = new DynamoDB(Configuration.DB_CLIENT).getTable(TABLE_NAME);

	public UserModel(String userId, String accessToken) {
		this.userId = userId;
		this.accessToken = accessToken;
	}

	public boolean exists() {
		return table.query(USER_ID, userId).firstPage().size() > 0;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public ArrayList<PermissionGrant> getDeniedPermissions(FacebookClient fbClient) {
		JsonArray permissions = fbClient.fetchObject(userId + "/permissions", JsonObject.class)
		                                .getJsonArray("data");
		ArrayList<PermissionGrant> deniedGrants = new ArrayList<>();
		for (int i = 0; i < permissions.length(); i++) {
			JsonObject permission = permissions.getJsonObject(i);
			String permName = permission.getString("permission"),
			        grant = permission.getString("status");
			if (!grant.equalsIgnoreCase("granted")) {
				deniedGrants.add(new PermissionGrant(permName, grant));
			}
		}
		return deniedGrants;
	}

	public String getUserId() {
		return userId;
	}

	public UpdateItemOutcome updateItem() {
		Map<String, Object> valueMap = new HashMap<>();
		Map<String, String> nameMap = new HashMap<>();
		valueMap.put(":" + ACCESS_TOKEN, accessToken);
		nameMap.put("#A", ACCESS_TOKEN);

		UpdateItemSpec spec = new UpdateItemSpec();
		spec.withNameMap(nameMap);
		spec.withValueMap(valueMap);
		spec.withUpdateExpression("SET #A = :" + ACCESS_TOKEN);
		spec.withReturnValues(ReturnValue.ALL_OLD);
		spec.withPrimaryKey(USER_ID, userId);

		return table.updateItem(spec);
	}
}
