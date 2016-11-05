package scraper;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import database.DB;
import res.Matcher;
import res.MatcherPromotions;
import res.MatcherWorkers;


/**
 * The WestlingScraper scrapes the data from the website cagematch.net
 * 
 * The fetched data is stored in an sql db, which is specified in the DB.java class
 *
 * The Scraper doesn't have to fetch all the data at once, but can pick up at a point until where the db is filled. (very basic)
 */
public class WrestlingScraper {
	private static DB db;
	private static Matcher matcher;
	private static HtmlToPlainText htmlPlain; // to convert html to plain text which can be added to the db
	
	/* data_specs
	 * A List that specifies the outlines of the data to crawl
	 * elements -> A hash Map with the specifications: [String sql_create_sheet,       #the sql create sheet has the sql command to create the table
	 * 											    	String table_name, 
	 * 													Matcher matcher,
	 * 											    	boolean simple_table,
	 * 													String raw_link,               #link to the site where the links are fetched from
	 * 											    	int nr_of_values, 
	 * 											    	int step_size
	 * 													]
	 */
	private static ArrayList<HashMap<String, Object>> data_specs = new ArrayList<HashMap<String, Object>>() {{
		   add(new HashMap<String, Object>() {{
			   put("sql_sheet", "sql_create_promotions.txt");
		   	   put("table_name", "Promotions");
		   	   put("matcher", new MatcherPromotions());
		   	   put("simple_table", true);
		   	   put("raw_link", "http://www.cagematch.net/?id=8&view=promotions");
		   	   put("nr_elements", new Integer(1866));
		   	   put("step_size", new Integer(100));
		    }});
		   add(new HashMap<String, Object>() {{
			   put("sql_sheet", "sql_create_workers.txt");
		   	   put("table_name", "Workers");
		   	   put("matcher", new MatcherWorkers());
		   	   put("simple_table", true);
		   	put("raw_link", "http://www.cagematch.net/?id=2&view=workers");
		   	   put("nr_elements", new Integer(17760));
		   	   put("step_size", new Integer(100));
			}});
		   
		}};

	int[] a = {1,1,2};
		
	public static void main(String[] args) {
		
		// establish connection to the database
		db = new DB();
		//create the tables in database if they don't already exist
		try {
			for(HashMap<String, Object> specs : data_specs) {
				db.createTableFromTxt("src/res/" + specs.get("sql_sheet"));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * Go through the data_specs and crawl/scrape all the information
		 */
		try {
			for(HashMap<String, Object> specs : data_specs) {
				
				// First get number of rows in table
				int count = 0;
				PreparedStatement stmtCount = db.conn.prepareStatement("SELECT COUNT(*) AS total FROM " + specs.get("table_name"));
				ResultSet rs = stmtCount.executeQuery();
				while(rs.next()) {
					count = rs.getInt("total");
				}
				if(count >= (Integer) specs.get("nr_elements")) {
					// all the data is already crawled/scraped
					System.out.println("'" + specs.get("table_name") + "' data alrady found!");
				} else {
					// Data has to be crawled. But if there already is some, start there!
					//first extract the links, where to crawl
					System.out.println("--- '" + specs.get("table_name") + "' starts at " + Integer.toString(count) + " ---");
					List<String> links = getLinksFromMultiSiteTable((String) specs.get("raw_link"),
							"&s=",
							count - 1, 
							(Integer) specs.get("step_size"),
							(Integer) specs.get("nr_elements"));
					
					System.out.println("Retrieved links for '" + specs.get("table_name") + "'");
					
					int j = 0;
					for(String link : links) {
						if(j%100 == 0) {
							System.out.print("Data Extraction: " + ((float )j)/ (Integer) specs.get("nr_elements") + "%   \r");
						}
						try {
							extractOverviewTableData(link, (String) specs.get("table_name"), (Matcher) specs.get("matcher"));
							j++;
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
			}
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			System.out.println("Error!");
			e2.printStackTrace();
		}
		
		
		
		
	}
	
	/**
	 * This method extracts all the links to the "sites/url's" to the single elements. Since all
	 * the tables are build similar this method can be used for all the link extraction
	 * 
	 * @param link : the basic link
	 * @param appendix : the appendix for following sites/url's
	 * @param stepSize : the step size of the table
	 * @return a list with links to all the elements of the table (e.g. Workers, Promotions etc.)
	 */
	public static List<String> getLinksFromMultiSiteTable(String link, String appendix, int start, int stepSize, int numberOfElements) {
		List<String> links = new ArrayList<String>();
		Document document = null;
		//after the first batch a appendix + step size is needed to connect to the link
		for(int i=start;i <= numberOfElements; i += stepSize) {
			System.out.print("Link Extraction: " + ((float )i)/numberOfElements + "% \r");
			try {
				document = Jsoup.connect(link+appendix+Integer.toString(i)).timeout(20 * 1000).get();
				
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(link+appendix+Integer.toString(i));
			}
			Elements rows = document.getElementsByClass("TRow");
			for(Element row : rows) {
				links.add("http://www.cagematch.net/" + row.child(2).child(0).attr("href"));;
			}
		}
		return links;
	}
	
	
	/**
	 * Extract the data from a simple table in the overview of Elements
	 * 
	 * @param link : link to the site with the overview
	 * @throws SQLException if a problem with the sql connection arises
	 */
	public static void extractOverviewTableData(String link, String table, Matcher matcher) throws SQLException {
		Document document = null;
		String db_field_name, value;
		String sql_to_insert = "`url`";
		String sql_values = "\"" + link + "\"";
		try {
			document = Jsoup.connect(link).timeout(20*1000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements rows = document.getElementsByClass("InformationBoxRow");
		for(Element row : rows) {
			db_field_name = matcher.match(row.child(0).text());
			if (db_field_name != null) {
				value = StringEscapeUtils.escapeHtml4(row.child(1).text());
				sql_values += ", \"" + value + "\"";
				sql_to_insert += ", `" + db_field_name + "`";
			}
		}
		 
		// put the sql command together and execute the query
		String sql = "INSERT INTO `wrestling`.`" + table + "` (" + sql_to_insert + ") VALUES (" + sql_values + ");";
		PreparedStatement stmt = db.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		stmt.execute();
	}

}
