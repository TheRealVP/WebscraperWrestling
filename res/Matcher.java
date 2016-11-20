package res;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * The Matcher, maps the labels of the website to the corresponding field in the db.
 *
 *
 */
public class Matcher {
	
	protected Map<String, String> map; 
	
	public Matcher() {
		//here it would be possible to read matching files
		System.out.println("Matcher successfully initialized");
	}
	
	public String match(String key) {
		return map.get(key);
	}
	
	
}
