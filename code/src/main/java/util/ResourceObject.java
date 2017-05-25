package util;

import java.util.Map;

/**
 * Represents a basic resource object as defined by the JSON API specification.
 * @author Dário T A
 */
public class ResourceObject {
	
	public final String id, type;
	public final Map<String, String> attributes;

	public ResourceObject(String id, String type) {
		this(id, type, null);
	}

	public ResourceObject(String id, String type, Map<String, String> attributes) {
		this.id = id;
		this.type = type;
		this.attributes = attributes;
	}
}
