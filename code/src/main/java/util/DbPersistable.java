package util;

import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;

public interface DbPersistable {

	public PutItemOutcome putItem();

}
