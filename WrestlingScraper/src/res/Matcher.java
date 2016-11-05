package res;

import java.util.HashMap;
import java.util.Map;

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
