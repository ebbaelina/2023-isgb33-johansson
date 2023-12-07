package se.kau.isgb33;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame; 
import javax.swing.JTextArea; 
import javax.swing.JTextField; 


public class Stub {

	public static void main(String[] args) {
		//klass som loggar det vi gör och skrivs i konsolen 
		Logger logger = LoggerFactory.getLogger(Stub.class);

		JFrame f = new JFrame("Movie Suggestions");
		f.setSize(400,500);
		f.setLayout(null);

		JTextArea area = new JTextArea(); 
		area.setLineWrap(true);
		area.setBounds(10,10,365,400);

		JTextField t = new JTextField(""); 
		t.setBounds(10,415,260,40);

		JButton b = new JButton("Search on genre"); 
		b.setBounds(275,415, 100, 40);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				String connString;

				try (InputStream input = new FileInputStream("connection.properties")) {

					Properties prop = new Properties();
					prop.load(input);
					connString = prop.getProperty("db.connection_string");
					logger.info(connString);

					ConnectionString connectionString = new ConnectionString(connString);
					MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString)
							.build();
					MongoClient mongoClient = MongoClients.create(settings);
					MongoDatabase database = mongoClient.getDatabase(prop.getProperty("db.name"));
					MongoCollection<Document> collection = database.getCollection("movies");

					//gör om första bokstaven i inputen till stor bokstav
					String genre = StringUtils.capitalize(t.getText());

					//filter som ska in i aggregatet
					Bson filter = Filters.in("genres", genre); 

					//Här skapas aggrigeringsanropet
					AggregateIterable<Document> myDocs = collection.aggregate(Arrays.asList(

							Aggregates.match(filter),
							//Projections.fields för att kunna köra include och exclude i samma projection
							Aggregates.project(Projections.fields(
									Projections.include("title", "year"),
									Projections.exclude("_id")
									)),
							Aggregates.limit(10),
							Aggregates.sort(Sorts.ascending("title"))
							)); 

					//iterator som går igenom svaret från api:t utifrån aggregeringsanropet
					MongoCursor<Document> iterator = myDocs.iterator();

					area.setText(""); 

					//kollar om iterator innerhåller några dokument
					if(iterator.hasNext()) {
						while(iterator.hasNext()) {
							Document myDoc = iterator.next(); 

							area.append(myDoc.getString("title") + ", ");
							area.append(myDoc.getInteger("year").toString() + "\n");}

						//Om iterator inte innerhåller något skrivs det ut i textarea

					}else {
						area.append("Ingen film matchade kategorin " + genre); 
					}

				} catch (FileNotFoundException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		f.add(area); 
		f.add(t); 
		f.add(b); 
		f.setVisible(true);


	}

}
