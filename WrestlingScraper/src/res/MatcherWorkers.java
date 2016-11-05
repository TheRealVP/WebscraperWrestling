package res;

import java.util.HashMap;

public class MatcherWorkers extends Matcher{
	
	public MatcherWorkers() {
		this.map = new HashMap<String, String>() {{
			   put("Current gimmick:", "current_name");
			   put("Promotion:", "current_promotion");
			   put("Birthplace:", "birthplace");
			   put("Gender:", "gender");
			   put("Height:", "height");
			   put("Weight:", "weight");
			   put("Relatives in wrestling:", "relatives_in_wrestling");
			   put("Background in sports:", "background_in_sports");
			   put("WWW:", "www");
			   put("Alter egos:", "alter_egos");
			   put("Roles:", "roles");
			   put("Beginning of in-ring career:", "start_of_career");
			   put("Wrestling style:", "wrestling_style");
			   put("Trainer:", "trainer");
			   put("Nickname(s):", "nicknames");
			   put("Trademark holds:", "trademark_holds");
			   put("Marital partner:", "marital_partner");
			   put("Obit:", "obit");
			   put("Cause of death:", "cause_of_death");
			   put("End of in-ring career:", "end_of_career");
			   
			   
			}};
	}
}
