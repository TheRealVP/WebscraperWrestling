package res;

import java.util.HashMap;

public class MatcherTitles extends Matcher{
	
	public MatcherTitles() {
		this.map = new HashMap<String, String>() {{
			   put("Current name:", "current_name");
			   put("Status:", "status");
			   put("Names:","names");
			   put("Promotions:", "promotions");
			}};
	}
}
