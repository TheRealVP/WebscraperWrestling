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
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import database.DB;
import res.Matcher;
import res.MatcherEvents;
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
		   	   put("matcher", new MatcherPromotions());
		   	   put("name", "Promotion");
		   	   put("raw_link", "http://www.cagematch.net/?id=8&view=promotions");
		   	   put("nr_elements", new Integer(2));
		   	   put("step_size", new Integer(100));
		   	   put("appendix", "&s=");
		   	   put("appendix_element", ""); // appendix during crawling of single elements
		    }});
		   add(new HashMap<String, Object>() {{
		   	   put("matcher", new MatcherWorkers());
		   	   put("name", "Worker");
		   	   put("raw_link", "http://www.cagematch.net/?id=2&view=workers");
		   	   put("nr_elements", new Integer(2));
		   	   put("step_size", new Integer(2));
		   	   put("appendix", "&s=");
		   	   put("appendix_element", ""); // appendix during crawling of single elements
			}});
		   add(new HashMap<String, Object>() {{
		   	   put("matcher", new MatcherEvents());
		   	   put("name", "Event");
		   	   put("raw_link", "http://www.cagematch.net/?id=1&view=results");
		   	   put("nr_elements", new Integer(2));
		   	   put("step_size", new Integer(2));
		   	   put("appendix", "&s="); // appendix during link extraction
		   	   put("appendix_element", ""); // appendix during crawling of single elements
			}});
		   
		}};
		
	public static void main(String[] args) {
		
		// establish connection to the database
		//create the tables in database if they don't already exist

		

		for(HashMap<String, Object> specs : data_specs) {
			
			
			// Data has to be crawled. But if there already is some, start there!
			//first extract the links, where to crawl
			System.out.println("--- '" + specs.get("name") + " ---");
			List<String> links = getLinksFromMultiSiteTable((String) specs.get("raw_link"),
					(String) specs.get("appendix"),
					(Integer) specs.get("step_size"),
					(Integer) specs.get("nr_elements"));
			
			System.out.println("Retrieved links for '" + specs.get("name") + "'");
			
			int j = 0;
			for(String link : links) {
				if(j%100 == 0) {
					System.out.print("Data Extraction: " + ((float )j)/ (Integer) specs.get("nr_elements") + "%   \r");
				}
				extractOverviewTableData(link, (String) specs.get("appendix_element"), (String) specs.get("table_name"), (Matcher) specs.get("matcher"));
				j++;
			}
			
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
	public static List<String> getLinksFromMultiSiteTable(String link, String appendix, int stepSize, int numberOfElements) {
		List<String> links = new ArrayList<String>();
		Document document = null;
		//after the first batch a appendix + step size is needed to connect to the link
		for(int i=0;i <= numberOfElements; i += stepSize) {
			System.out.print("Link Extraction : " + ((float )i)/numberOfElements + "% \r");
			try {
				document = Jsoup.connect(link+appendix+Integer.toString(i)).timeout(20 * 1000).get();
				
			} catch (IOException e) {
				e.printStackTrace();
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
	public static void extractOverviewTableData(String link, String appendix, String table, Matcher matcher) {
		Document document = null;
		String field_name;
		String value;
		try {
			document = Jsoup.connect(link + appendix).timeout(20*1000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements rows = document.getElementsByClass("InformationBoxRow");
		for(Element row : rows) {
			field_name = matcher.match(row.child(0).text());
			if (field_name != null) {
				value = StringEscapeUtils.escapeHtml4(row.child(1).text());
				
//				System.out.print
				
			}
		}
	}
	
	

}
