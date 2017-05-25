package login;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.google.gson.Gson;

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

	public static final String TABLE_NAME = "users", USER_ID = "userId", ACCESS_TOKEN = "token",
	        SCOPES = "scopes";
	private static final Table TABLE = new DynamoDB(Configuration.DB_CLIENT).getTable(TABLE_NAME);
	private static final Gson SERIALISER = new Gson();

	private String userId, accessToken;
	private List<String> scopes;

	public UserModel(String userId, String accessToken) {
		this(userId, accessToken, null);
	}

	public UserModel(String userId, String accessToken, List<String> scopes) {
		this.userId = userId;
		this.accessToken = accessToken;
		this.scopes = scopes;
	}

	public boolean exists() {
		return TABLE.query(USER_ID, userId).firstPage().size() > 0;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getUserId() {
		return userId;
	}

	public boolean hasScope(String scope) {
		return scopes.contains(scope);
	}

	@SuppressWarnings("unchecked")
	public void retrieveScopes() {
		Item item = TABLE.getItem(new PrimaryKey(USER_ID, userId));
		scopes = SERIALISER.fromJson(item.getString(SCOPES), ArrayList.class);
	}

	public UpdateItemOutcome updateItem() {
		Map<String, Object> valueMap = new HashMap<>();
		Map<String, String> nameMap = new HashMap<>();
		valueMap.put(":" + ACCESS_TOKEN, accessToken);
		valueMap.put(":" + SCOPES, SERIALISER.toJson(scopes));
		nameMap.put("#A", ACCESS_TOKEN);
		nameMap.put("#S", SCOPES);

		UpdateItemSpec spec = new UpdateItemSpec();
		spec.withNameMap(nameMap);
		spec.withValueMap(valueMap);
		spec.withUpdateExpression("SET #A = :" + ACCESS_TOKEN);
		spec.withUpdateExpression("SET #S = :" + SCOPES);
		spec.withReturnValues(ReturnValue.ALL_OLD);
		spec.withPrimaryKey(USER_ID, userId);

		return TABLE.updateItem(spec);
	}
}
