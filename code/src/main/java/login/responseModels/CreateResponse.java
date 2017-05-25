package login.responseModels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import login.UserModel;
import util.ResourceObject;

public class CreateResponse {
	
	public final ResourceObject[] data;
	public final Map<String, Object> meta = new HashMap<>();
	
	public CreateResponse(HashSet<String> permissions, UserModel user) {
		data = new ResourceObject[permissions.size()];
		permissions.toArray(data);
		meta.put("accessInfo", user);
	}
	
	public CreateResponse(UserModel user) {
		this(new HashSet<>(), user);
	}

}
