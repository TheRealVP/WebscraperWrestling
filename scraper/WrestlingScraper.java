package scraper;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
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
import res.MatcherTitles;
import res.MatcherWorkers;

//changed this line hue
/**
 * The WestlingScraper scrapes the data from the website cagematch.net
 * 
 * The fetched data is stored in an sql db, which is specified in the DB.java class
 *
 * The Scraper doesn't have to fetch all the data at once, but can pick up at a point until where the db is filled. (very basic)
 */
public class WrestlingScraper {								/*WATCH OUT FOR  VVV */
	/*CHANGE THE PATH JOLAN!!!*/ private static String ontfile= "C:\\Users\\Vasco\\git\\WebscraperWrestling\\WrestlingScraper\\wrestling.rdf";
	private static HtmlToPlainText htmlPlain; // to convert html to plain text which can be added to the db
	//http://www.semanticweb.org/vasco/ontologies/2016/9/wrestling

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
	private static String NS= "http://www.semanticweb.org/vasco/ontologies/2016/9/untitled-ontology-5";
	private static OntModel m = ModelFactory.createOntologyModel();
	private static OntDocumentManager dm = m.getDocumentManager();
	private static int entityExtracted;

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
		/*		   add(new HashMap<String, Object>() {{
		   	   put("matcher", new MatcherEvents());
		   	   put("name", "Event");
		   	   put("raw_link", "http://www.cagematch.net/?id=1&view=results");
		   	   put("nr_elements", new Integer(2));
		   	   put("step_size", new Integer(2));
		   	   put("appendix", "&s="); // appendix during link extraction
		   	   put("appendix_element", ""); // appendix during crawling of single elements
			}});*/
		add(new HashMap<String, Object>() {{
			put("matcher", new MatcherTitles());
			put("name", "Title");
			put("raw_link", "http://www.cagematch.net/?id=5&view=titles");
			put("nr_elements", new Integer(2));
			put("step_size", new Integer(2));
			put("appendix", "&s=");
			put("appendix_element", ""); // appendix during crawling of single elements
		}});
	}};

	public static void main(String[] args) {

		// establish connection to the database
		//create the tables in database if they don't already exist


		dm.addAltEntry( "http://www.semanticweb.org/vasco/ontologies/2016/9/wrestling",
				"file:" + ontfile );
		m.read("http://www.semanticweb.org/vasco/ontologies/2016/9/wrestling");
		for(HashMap<String, Object> specs : data_specs) {


			// Data has to be crawled. But if there already is some, start there!
			//first extract the links, where to crawl
			System.out.println("--- '" + specs.get("name") + " ---");
			List<String> links = getLinksFromMultiSiteTable((String) specs.get("raw_link"),
					(String) specs.get("appendix"),
					(Integer) specs.get("step_size"),
					(Integer) specs.get("nr_elements"));

			System.out.println("Retrieved links for '" + specs.get("name") + "'");
			switch(specs.get("name").toString())
			{
			case "Promotion":
				entityExtracted=0;
				break;
			case "Event":
				entityExtracted=1;
				break;
			case "Worker":
				entityExtracted=2;
				break;
			case "Title":
				entityExtracted=3;
				break;
			}
			int j = 0;
			for(String link : links) {
				if(j%100 == 0) {
					System.out.print("Data Extraction: " + ((float )j)/ (Integer) specs.get("nr_elements") + "%   \r");
				}

				//m.write(System.out,"RDF/XML");
				extractOverviewTableData(link, (String) specs.get("appendix_element"), (String) specs.get("table_name"), (Matcher) specs.get("matcher"));

				j++;
			}

		}

		OutputStream out;
		try {
			out = new FileOutputStream("fullwrestling.rdf");
			m.write(out);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		//ascjdoajd
		//aksfjdl;askdfj
		//asdfkl;adjs
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
		boolean restart=true;
		try {
			document = Jsoup.connect(link + appendix).timeout(20*1000).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements rows = document.getElementsByClass("InformationBoxRow");

		Individual promo=null;
		Individual rassler=null;
		Individual title=null;

		
		String name = document.select("h1.TextHeader").first().text();


		for(Element row : rows) {

			field_name = matcher.match(row.child(0).text());
			if (field_name != null) {
				value = StringEscapeUtils.escapeHtml4(row.child(1).text());
				StringTokenizer tk= new StringTokenizer(value,",");
				String token;
				System.out.println(field_name + " : " + value);
				if(entityExtracted==0)

				{
					if(field_name.equals("current_name"))
					{
						//name= NS + "#"+ value;
						promo= m.createIndividual(NS + "#"+ name.replaceAll("%","percent"), m.getOntClass(NS +"#Promotion"));
						System.out.println(promo.toString());
						promo.addProperty(m.getProperty(NS + "#hasName"), value);

					}
					else
					{
						switch(field_name)
						{
						case "current_abbrev":
							promo.addProperty(m.getProperty(NS + "#hasAbbreviation"),value);

							break;
						case "status":
							promo.addProperty(m.getProperty(NS + "#hasStatus"),value);

							break;
						case "location":
							promo.addProperty(m.getProperty(NS + "#hasLocation"),value);

							break;
						case "active_time":
							promo.addProperty(m.getProperty(NS + "#hasActiveTime"),value);

							break;
						case "owners":
							Individual owner=m.createIndividual(NS +"#"+value, m.getOntClass(NS +"#Owner"));
							owner.addProperty(m.getProperty(NS + "#hasName"),value);
							promo.addProperty(m.getProperty(NS + "#hasOwner"),owner);

							break;
						case "popular_events":
							tk= new StringTokenizer(value,",");


							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								Individual event=m.createIndividual(NS +"#"+token, m.getOntClass(NS +"#Event"));
								event.addProperty(m.getProperty(NS + "#hasName"),token);
								promo.addProperty(m.getProperty(NS + "#hasEvent"),event);
							}
							break;
						case "tv_shows":
							tk= new StringTokenizer(value,",");


							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								Individual event=m.createIndividual(NS +"#"+token, m.getOntClass(NS +"#Show"));
								event.addProperty(m.getProperty(NS + "#hasName"),token);
								promo.addProperty(m.getProperty(NS + "#hasShow"),event);
							}

							break;
						default:
							break;
						}

					}
				}
				else if(entityExtracted==2)
				{
					if(restart==true)
					{

						rassler= m.createIndividual(NS + "#"+ name.replaceAll("%","percent"), m.getOntClass(NS +"#Wrestler"));
						System.out.println("Rassler Name: " + rassler.toString());
						rassler.addProperty(m.getProperty(NS + "#hasName"), name);
						restart=false;
					}
					else
					{
						System.out.println("Field Name:" + field_name);
						switch(field_name)
						{

						case "current_promotion":

							if(m.getIndividual(NS+"#"+value)!=null)
							{
								rassler.addProperty(m.getProperty(NS+"#hasPromotion"), m.getIndividual(NS+"#"+value));
							}
							else
							{
								Individual boss= m.createIndividual(NS + "#"+ value.replaceAll("%","percent"), m.getOntClass(NS +"#Promotion"));
								//System.out.println(promo.toString());
								boss.addProperty(m.getProperty(NS + "#hasName"), value);
								rassler.addProperty(m.getProperty(NS+"#hasPromotion"), boss);
							}
							break;
						case "birthplace":
							rassler.addProperty(m.getProperty(NS+"#hasBirthplace"),value);
							break;
						case "gender":
							rassler.addProperty(m.getProperty(NS+"#hasGender"),value);
							break;
						case "height":
							rassler.addProperty(m.getProperty(NS+"#hasHeight"),value);
							break;
						case "weight":
							rassler.addProperty(m.getProperty(NS+"#hasWeight"),value);
							break;
						case "relatives_in_wrestling":
							tk= new StringTokenizer(value,",");
							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								if(m.getIndividual(NS+"#"+token)!=null)
								{
									rassler.addProperty(m.getProperty(NS+"#isRelated"), m.getIndividual(NS+"#"+token));
								}
								else
								{
									Individual relative=m.createIndividual(NS +"#"+token, m.getOntClass(NS +"#Person"));
									relative.addProperty(m.getProperty(NS + "#hasName"),token);
									rassler.addProperty(m.getProperty(NS + "#isRelated"),relative);
								}
							}
							break;
						case "background_in_sports":
							rassler.addProperty(m.getProperty(NS+"#hasSportsBackground"), value);
							break;
						case "alter_egos":
							tk= new StringTokenizer(value,",");
							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								if(m.getIndividual(NS+"#"+token)!=null)
								{
									rassler.addProperty(m.getProperty(NS+"#isAlterEgo"), m.getIndividual(NS+"#"+value));
								}
								else
								{
									Individual alter=m.createIndividual(NS +"#"+token, m.getOntClass(NS +"#Wrestler"));
									alter.addProperty(m.getProperty(NS + "#hasName"),token);
									rassler.addProperty(m.getProperty(NS + "#isAlterEgo"),alter);
									alter.addProperty(m.getProperty(NS + "#isAlterEgo"),rassler);
								}
								//rassler.addProperty();
							}
							break;
							/*case "roles":
							tk= new StringTokenizer(value,",");
							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								rassler.addProperty("", );
							}
							break;*/
						case "start_of_career":
							rassler.addProperty(m.getProperty(NS+"#beginningOfCareer"),value);
							break;
						case "wrestling_style":
							rassler.addProperty(m.getProperty(NS+"#hasStyle"), value);
							break;
						case "trainer":
							if(m.getIndividual(NS+"#"+value)!=null)
							{
								rassler.addProperty(m.getProperty(NS+"#hasTrainer"), m.getIndividual(NS+"#"+value));
							}
							else
							{
								Individual alter=m.createIndividual(NS +"#"+value, m.getOntClass(NS +"#Wrestler"));
								alter.addProperty(m.getProperty(NS + "#hasName"),value);
								rassler.addProperty(m.getProperty(NS + "#hasTrainer"),alter);
							}
							break;
						case "nicknames":
							tk= new StringTokenizer(value,",");
							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								rassler.addProperty(m.getProperty(NS + "#hasNickname"), value);
							}
							break;
						case "trademark_holds":
							tk= new StringTokenizer(value,",");
							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								rassler.addProperty(m.getProperty(NS + "#hasTrademarkHold"), value);
							}
							break;
						case "marital_partner":
							if(m.getIndividual(NS+"#"+value)!=null)
							{
								rassler.addProperty(m.getProperty(NS+"#hasMaritalPartner"), m.getIndividual(NS+"#"+value));
								m.getIndividual(NS+"#"+value).addProperty(m.getProperty(NS+"#hasMaritalPartner"), rassler);
							}
							else
							{
								Individual alter=m.createIndividual(NS +"#"+value, m.getOntClass(NS +"#Person"));
								alter.addProperty(m.getProperty(NS + "#hasName"),value);
								rassler.addProperty(m.getProperty(NS + "#hasMaritalPartner"),alter);
								alter.addProperty(m.getProperty(NS + "#hasMaritalPartner"),rassler);
							}
							break;
						case "obit":
							rassler.addProperty(m.getProperty(NS+"#hasObit"), value);
							break;
						case "cause_of_death":
							rassler.addProperty(m.getProperty(NS+"#hasCauseOfDeath"), value);
							break;
						case "end_of_career":
							rassler.addProperty(m.getProperty(NS+"#endOfCareer"), value);
							break;
						default:
							break;
						}
					}
				}
				else if(entityExtracted==3)
				{
					if(field_name.equals("current_name"))
					{
						//name= NS + "#"+ value;
						title= m.createIndividual(NS + "#"+ name.replaceAll("%","percent"), m.getOntClass(NS +"#Title"));
						System.out.println(title.toString());
						title.addProperty(m.getProperty(NS + "#hasName"), value);
					}
					else
					{
						switch(field_name)
						{
						case "status":
							if(value.toLowerCase().equals("active"))
								title.addProperty(m.getProperty(NS+ "#isActive"), "true");
							else
								title.addProperty(m.getProperty(NS+ "#isActive"), "false");
							break;
						case "promotions":
							tk= new StringTokenizer(value,"()");
							while(tk.hasMoreTokens())
							{
								token=tk.nextToken();
								if(m.getIndividual(NS+"#"+token)!=null)
									title.addProperty(m.getProperty(NS + "#hasPromotion"), m.getIndividual(NS+"#"+token));
								else
								{
									Individual putmedownplease= m.createIndividual(NS+"#"+token.replaceAll("%","percent"),m.getOntClass(NS+ "#Promotion"));
									putmedownplease.addProperty(m.getProperty(NS + "#hasName"), token);
									title.addProperty(m.getProperty(NS + "#hasPromotion"), putmedownplease);
								}
								tk.nextToken();
							}
							break;
							default: break;
						}
					}
				}
			}
		}
		if(entityExtracted==3)
		{
			Elements holders= document.getElementsByClass("TRow");
			
			for(Element row : holders)
			{
				String aargh= row.getElementsByClass("TextBold").text();
				StringTokenizer goaway= new StringTokenizer(aargh, "(");
				String actualname= goaway.nextToken();
				if(m.getIndividual(NS+"#"+actualname)!=null)
				{
					m.getIndividual(NS+"#"+actualname).addProperty(m.getProperty(NS+"#hasTitle"), title);
				}
				else
				{
					
					Individual noob= m.createIndividual(NS + "#"+ actualname.replaceAll("%","percent"), m.getOntClass(NS +"#Wrestler"));
					noob.addProperty(m.getProperty(NS + "#hasName"), actualname);
					noob.addProperty(m.getProperty(NS+"#hasTitle"), title);
				}
			}
		}
	}
}
