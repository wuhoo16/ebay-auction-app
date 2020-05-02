
/*
 * Author: Vallath Nandakumar and EE 422C instructors
 * Date: April 20, 2020
 * This starter code is from the MultiThreadChat example from the lecture, and is on Canvas. 
 * It doesn't compile.
 */

public class ChatClient extends Application { 
	// I/O streams 
	ObjectOutputStream toServer = null; 
	ObjectInputStream fromServer = null;


	@Override
	public void start(Stage primaryStage) { 
		BorderPane mainPane = new BorderPane(); 

		// Create a scene and place it in the stage 
		Scene scene = new Scene(mainPane, 450, 200); 
		primaryStage.setTitle("Client"); // Set the stage title 
		primaryStage.setScene(scene); // Place the scene in the stage 
		primaryStage.show(); // Display the stage 

		XX.setOnAction(e -> { 
		});  // etc.

		try { 
			// Create a socket to connect to the server 
			@SuppressWarnings("resource")
			Socket socket = new Socket("localhost", 8000); 

			// Create an input stream to receive data from the server 
			fromServer = new ObjectInputStream(socket.getInputStream()); 

			// Create an output stream to send data to the server 
			toServer = new ObjectOutputStream(socket.getOutputStream()); 
		} 
		catch (IOException ex) { 
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
