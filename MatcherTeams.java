package res;

import java.util.HashMap;

public class MatcherTeams extends Matcher{
	
	public MatcherTeams() {
		this.map = new HashMap<String, String>() {{
			   put("Promotions:", "promotions");
			   put("Years active:", "years");
			   put("Trademark holds:","holds");
			}};
	}
}
