package login;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;

import util.Configuration;
import util.DbPersistable;

public class LoginModel implements DbPersistable {

	private static final Table TABLE = new DynamoDB(Configuration.DB_CLIENT).getTable("loginTokens");
	private static final String USER_ID = "userId", TOKEN = "token";

	public final String userId;
	public final String token;

	public LoginModel(String userId, String token) {
		this.userId = userId;
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public String getUserId() {
		return userId;
	}

	public boolean isValid() {
		if (token == null || userId == null) {
			return false;
		}
		return TABLE.query(TOKEN, token, new RangeKeyCondition(USER_ID).eq(userId)).firstPage()
		            .size() > 0;
	}

	@Override
	public PutItemOutcome putItem() {
		Item item = new Item();
		item.withString(USER_ID, userId);
		item.withString(TOKEN, token);

		return TABLE.putItem(item);
	}

}
