package res;

import java.util.HashMap;

public class MatcherEvents extends Matcher{
	
	public MatcherEvents() {
		this.map = new HashMap<String, String>() {{
			   put("Name of the event:", "name_of_event");
			   put("Date:", "date");
			   put("Promotion:", "promotion");
			   put("Type:", "type");
			   put("Location:", "location");
			   put("Arena:", "arena");
			   put("Broadcast type:", "broadcast_type");
			   put("Broadcast date:", "broadcast_date");
			   put("TV station/network:", "network");
			   put("Commentary by:", "commentators");
			   
			   
			}};
	}
}
