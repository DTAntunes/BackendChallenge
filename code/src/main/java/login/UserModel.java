package login;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.google.gson.Gson;

import util.Configuration;
import util.DbPersistable;

/**
 * Models a user by user ID and a long term access token used for requests.
 * Tracks denied permissions with that token, which is presumably the most
 * recently denied ones. Permissions can be revoked independent of the app, but
 * I'm ignoring that for now.
 *
 * @author Dário T A
 */
public class UserModel implements DbPersistable {

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

	public LoginModel getLoginData(String loginToken) {
		return new LoginModel(userId, loginToken);
	}

	public String getUserId() {
		return userId;
	}

	public boolean hasScope(String scope) {
		return scopes.contains(scope);
	}

	@Override
	public PutItemOutcome putItem() {
		Item item = new Item();
		item.withString(ACCESS_TOKEN, accessToken);
		item.withString(SCOPES, SERIALISER.toJson(scopes));
		item.withString(USER_ID, userId);

		return TABLE.putItem(item);
	}

	@SuppressWarnings("unchecked")
	public void retrieveScopes() {
		Item item = TABLE.getItem(new PrimaryKey(USER_ID, userId));
		scopes = SERIALISER.fromJson(item.getString(SCOPES), ArrayList.class);
	}
}
