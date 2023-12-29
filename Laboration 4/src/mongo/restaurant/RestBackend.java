package mongo.restaurant;



import static spark.Spark.get;
import static spark.Spark.post;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.text.WordUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;



public class RestBackend {

	public static void main(String[] args) throws IOException {
		String connString;
		Logger logger = LoggerFactory.getLogger(RestBackend.class);
		InputStream input = new FileInputStream("connection.properties");

		Properties prop = new Properties();
		prop.load(input);
		connString = prop.getProperty("db.connection_string");
		logger.info(connString);


		ConnectionString connectionString = new ConnectionString(connString);
		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
				.build();
		MongoClient mongoClient = MongoClients.create(settings);
		MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
		logger.info(prop.getProperty("db.name"));

		/*Should search for title in movie collection ("sample_mflx" database). 
		 * If available a JSON response containing all the values from the found JSON document minus (id, poster,cast and fullplot) together with the http 200 OK statuscode.
		 * If no movie matches the title in the movie collection the response should be 
		 * the http 404 statuscode together with a jsonformatted cleartext message.
		 */
		get("/title/:title", (req,res)->{
			MongoCollection<Document> collection = database.getCollection("movies");
			String filter=req.params("title").toLowerCase();
			filter=WordUtils.capitalizeFully(filter);
			logger.info("Filtrerar på title: " + filter);
			Document myDoc = collection
					.find(Filters.eq("title", filter))
					.first();
			logger.info("Värdet i myDoc: " + myDoc);
			if(myDoc != null) {
				logger.info("if");
				myDoc.remove("_id");
				myDoc.remove("poster");
				myDoc.remove("cast");
				myDoc.remove("fullplot");
				//skickar vår response till klienten
				return myDoc.toJson(); 
			}else{
				logger.info("else");
				res.status(404);
				res.type("application/json"); 
				JsonObject errorMsg = new JsonObject();
				errorMsg.addProperty("error", "Movie not found");
				//skickar felmeddelande till klienten
				return errorMsg.toString(); 
			} 
		});
		/*Should search for title in movie collection ("sample_mflx" database).
	       If available a JSON response containing title and fullplot together with the http 200 OK statuscode.
	       If no movie matches the title in the movie collection the response should be 
	       the http 404 statuscode together with a jsonformatted cleartext message.*/
		get("/fullplot/:title", (req,res)->{
			MongoCollection<Document> collection = database.getCollection("movies");
			String filter=req.params("title").toLowerCase();
			filter=WordUtils.capitalizeFully(filter);
			logger.info("Filtrerar på title: " + filter);
			Document myDoc = collection
					.find(Filters.eq("title", filter))
					.projection(Projections.include("title", "fullplot"))
					.first();
			logger.info("Värdet i myDoc: " + myDoc);
			if(myDoc !=null) {
				logger.info("if");
				myDoc.remove("_id"); 
				//skickar vår response till klienten
				return myDoc.toJson();
			}else {
				logger.info("else");
				res.status(404);
				res.type("application/json"); 
				JsonObject errorMsg = new JsonObject();
				errorMsg.addProperty("error", "Movie not found");
				//skickar felmeddelande till klienten
				return errorMsg.toString(); 
			}
		});
		/*Should search for title in movie collection ("sample_mflx" database).
		 *If available a JSON response containing title and the complete cast for the title together with the 
		 *http 200 OK statuscode.If no movie matches the title in the movie collection the response should be
		 *the http 404 statuscode together with a jsonformatted cleartext message.*/
		get("/cast/:title", (req,res)->{
			MongoCollection<Document> collection = database.getCollection("movies");
			String filter=req.params("title").toLowerCase();
			filter=WordUtils.capitalizeFully(filter);
			logger.info("Filtrerar på title: " + filter);
			Document myDoc = collection
					.find(Filters.eq("title", filter))
					.projection(Projections.include("title", "cast"))
					.first();
			logger.info("Värdet i myDoc: " + myDoc);
			if(myDoc !=null) {
				logger.info("if");
				myDoc.remove("_id"); 
				//skickar vår response till klienten
				return myDoc.toJson();
			}else {
				logger.info("else");
				res.status(404);
				res.type("application/json"); 
				JsonObject errorMsg = new JsonObject();
				errorMsg.addProperty("error", "Movie not found");
				//skickar felmeddelande till klienten
				return errorMsg.toString(); 
			}
		});
		/*Should search for movies with the provided genre in movie collection ("sample_mflx" database).
		 *If available a JSON response containing maximum 10 movies from the provided genre minus (id, poster,cast and fullplot)
		 *together with the http 200 OK statuscode.If no movies matches the genre in the movie collection 
		 *the response should be the http 404 statuscode together with a jsonformatted cleartext message.*/
		get("/genre/:genre", (req,res)->{
			MongoCollection<Document> collection = database.getCollection("movies");
			String filter=req.params("genre").toLowerCase();
			int limit = 10; 
			if(req.queryParams("limit") != null) {
				try {
					limit=Integer.parseInt(req.queryParams("limit"));	    		  
				}catch(NumberFormatException e){
				}
			}
			filter=WordUtils.capitalizeFully(filter);
			logger.info("Filterar genre på:" + filter); 
			MongoCursor<Document> cursor = collection
					.find(Filters.eq("genres", filter))
					.projection(Projections.exclude("_id", "poster", "cast", "fullplot"))
					.limit(limit)
					.iterator();
			JsonArray arr = new JsonArray(); 
			Document doc; 
			try {
				if(!cursor.hasNext()) {
					logger.info("else"); 
					res.status(404);
					res.type("application/json"); 
					JsonObject errorMsg = new JsonObject();
					errorMsg.addProperty("error", "Movie not found");
					//skickar felmeddelande till klienten
					return errorMsg.toString(); 
				}
				while(cursor.hasNext()) {
					logger.info("if"); 
					doc = cursor.next();
					doc.remove("_id"); 

					/*new Gson() converterar java object till Json. toJsonTree() tar vårt 
					java object(Document) och konverterar till ett JsonObject
					Gson ger en välformaterad Json string, som är rätt formaterad och onödig specialtecken */
					arr.add(new Gson().toJsonTree(doc)); 
				}	  
			}finally {
				logger.info("finally"); 
				cursor.close();
			}
			return arr;  
		});
		/*Should search for movies that provided actor acted in from the movie collection ("sample_mflx" database).
		 *If available a JSON response containing maximum 10 movie titles only together with the http 200 OK statuscode.
		 *If actor is not present in any movie the response should be the http 404 statuscode
		 *together with a jsonformatted cleartext message.*/
		get("/actor/:actor", (req,res)->{
			MongoCollection<Document> collection = database.getCollection("movies");
			String filter=req.params("actor").toLowerCase();
			int limit = 10; 
			if(req.queryParams("limit") != null) {
				try {
					limit=Integer.parseInt(req.queryParams("limit"));	    		  
				}catch(NumberFormatException e){
				}
			}
			filter=WordUtils.capitalizeFully(filter);
			logger.info("Filtrerar på title: " + filter);	

			MongoCursor<Document> cursor = collection
					.find(Filters.in("cast", filter))
					.projection(Projections.include("title"))
					.limit(limit)
					.iterator();


			JsonArray arr = new JsonArray(); 
			Document doc; 
			try {
				if(!cursor.hasNext()) {
					logger.info("No movies found for actor: " + filter); 
					res.status(404);
					res.type("application/json"); 
					JsonObject errorMsg = new JsonObject();
					errorMsg.addProperty("error", "Actor not found");
					//skickar felmeddelande till klienten
					return errorMsg.toString(); 
				}
				while(cursor.hasNext()) {
					logger.info("while cursor.hasNext()"); 
					doc = cursor.next();
					doc.remove("_id"); 

					/*new Gson() converterar java object till Json. toJsonTree() tar vårt 
					java object(Document) och konverterar till ett JsonObject
					Gson ger en välformaterad Json string, som är rätt formaterad och onödig specialtecken */
					arr.add(new Gson().toJsonTree(doc)); 
				}	  
			}finally {
				logger.info("finally"); 
				cursor.close();
			}
			return arr;  
		});
		/*Should add the movie represented by incoming json from the request to the movie collection ("sample_mflx" database).
		 *A successful addition be represented by an empty response and the http 202 Accepted statuscode. 
		 *If something goes wrong send an appropriate http error code and jsonformatted message as the respone.*/
		post("/title", (req,res)->{
			res.type("application/json");
			try {
				MongoCollection<Document> collection = database.getCollection("movies");
				//innehållet i inkommande värdet ligger i request.body och läser in den i dokumenet med motoden parse, lägger in det skapade dokumentet i vår collection document
	               collection.insertOne(new Document(Document.parse(req.body())));
	               logger.info("Adding new movie" + req.body()); 
			}catch(MongoException me) {
				logger.info("catch" + req.body()); 
				res.status(404);
				res.type("application/json"); 
				JsonObject errorMsg = new JsonObject();
				errorMsg.addProperty("error", "Unable to add movie to db");
				//skickar felmeddelande till klienten
				return errorMsg.toString(); 
			}
			res.status(202); 
			return "";
		});
	}
}
