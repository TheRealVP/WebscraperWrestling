package res;

import java.util.HashMap;

public class MatcherPromotions extends Matcher{
	
	public MatcherPromotions() {
		this.map = new HashMap<String, String>() {{
			   put("Current name:", "current_name");
			   put("Current abbreviation:", "current_abbrev");
			   put("Status:", "current_status");
			   put("Location:", "location");
			   put("Active Time:", "active_time");
			   put("WWW:", "www");
			   put("Names:", "all_names");
			   put("Abbreviations:", "all_abbrev");
			   put("Owners:", "owners");
			   put("Popular events:", "popular_events");
			   put("Television shows:", "tv_shows");
			}};
	}
	
	
}
