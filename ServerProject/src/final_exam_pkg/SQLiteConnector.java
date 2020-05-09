/*  SQLiteConnector.java
 *  EE422C Final Project submission by
 *  Andy Wu
 *  amw5468
 *  16295
 *  Spring 2020
 */
package final_exam_pkg;

import java.sql.DriverManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

class SQLiteConnecter {
	private ArrayList<Item> itemList = new ArrayList<Item>(); 
    
	/**
	 * Creates and returns a connection to the SQLite database named "itemDatabase.db".
	 * NOTE: This database file must be named exactly as specified and has to be placed in the final_exam_pkg for the ServerProject in order for the jdbc driver to locate the database.
	 *
	 * @author sqlitetutorial.net / TODO: (make sure to add this to the References in the documentation report)
	 * @return Connection object to the SQLite database containing all auction item info.
	 */
    protected Connection connect() {
        // SQLite database connection string with relative pathname
    	// NOTE: itemDatabase MUST be in the same package as this SQLiteConnector class file
    	String url = "jdbc:sqlite:src/final_exam_pkg/itemDatabase.db";
        Connection connection = null;
        try {
        	connection = DriverManager.getConnection(url);
        	connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return connection;
    }
   
    
    /**
     * Select all items in the SQLite table named TBL_Auction_Items and populates the Server's item arraylist.
     */
    protected ArrayList<Item> getAllDatabaseItems(){
        String selectQuery = "SELECT Name, Description, MinPrice, BuyNowPrice, Duration FROM TBL_Auction_Items";
        
        try (
        	 Connection connection = this.connect();
             Statement stmt = connection.createStatement();
             ResultSet resultData = stmt.executeQuery(selectQuery)){
            
            // loop through the result set of executing the sql query above
            while (resultData.next()) {
            	String name = resultData.getString("Name");
            	String description = resultData.getString("Description");
            	double minPrice = resultData.getDouble("MinPrice");
            	double buyNowPrice = resultData.getDouble("BuyNowPrice");
            	BigDecimal duration = new BigDecimal(resultData.getDouble("Duration"));
            	
            	itemList.add(new Item(name, description, minPrice, buyNowPrice, duration));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        
        return itemList;
    }
    
}

