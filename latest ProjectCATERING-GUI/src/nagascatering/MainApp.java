package nagascatering;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Objects; // Import Objects
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;


public class MainApp extends Application {

     private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

     // Define path constants
     private static final String MAIN_VIEW_FXML = "/nagascatering/view/MainView.fxml";
     private static final String STYLES_CSS = "/nagascatering/view/styles.css";

    @Override
    public void start(Stage primaryStage) {
        Parent root = null;
        try {
            // --- Use Absolute Path from the root of the classpath ---
            URL fxmlUrl = getClass().getResource(MAIN_VIEW_FXML);

            if (fxmlUrl == null) {
                 // Throw a specific error if the resource isn't found
                 throw new FileNotFoundException("Cannot find FXML resource: " + MAIN_VIEW_FXML + ". Check the path and build configuration.");
            }
            // --- End Path Check ---

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            root = loader.load(); // Load the FXML, controller is instantiated here

            // Controller instance is obtained if needed, but setup is often done in controller's initialize
            // MainController mainController = loader.getController();

            Scene scene = new Scene(root);

            // --- Load CSS ---
             URL cssUrl = getClass().getResource(STYLES_CSS);
             if (cssUrl != null) {
                 scene.getStylesheets().add(cssUrl.toExternalForm());
                 LOGGER.info("CSS loaded successfully from: " + cssUrl.toExternalForm());
             } else {
                 LOGGER.log(Level.WARNING, "Could not load styles.css from path: {0}", STYLES_CSS);
                 // Show a non-critical warning if CSS fails (optional)
                 // showAlert(Alert.AlertType.WARNING, "Styling Warning", "Could not load application stylesheet. Default styles will be used.");
             }
             // --- End Load CSS ---

            primaryStage.setTitle("Naga's Catering & Services Planner");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800); // Optional: Set minimum window size
            primaryStage.setMinHeight(600);
            primaryStage.show();

        } catch (IOException | IllegalStateException e) { // Catch relevant FXML loading exceptions
             LOGGER.log(Level.SEVERE, "Failed during application start (FXML Loading/Initialization)", e);
             showCriticalError("Application Load Error", "Could not load the main application window.", e);
             // Ensure exit happens even if platform exit fails
             System.exit(1);
        } catch (Exception e) { // Catch any other unexpected errors during startup
            LOGGER.log(Level.SEVERE, "Unexpected error during application start", e);
            showCriticalError("Application Error", "An unexpected error occurred during startup.", e);
            System.exit(1);
        }
    }

     // Helper method to show critical errors during startup
     private void showCriticalError(String title, String message, Exception ex) {
         // Use Platform.runLater to ensure it runs on the FX thread if available
         // Log the error regardless, as runLater might not execute if FX platform fails early
         LOGGER.log(Level.SEVERE, title + ": " + message, ex);

         Platform.runLater(() -> {
             Alert alert = new Alert(Alert.AlertType.ERROR);
             alert.setTitle(title);
             alert.setHeaderText(message);
             String content = "The application cannot start.\nPlease check the logs for more details.";
             if (ex != null) {
                 content += "\n\nError: " + ex.getClass().getSimpleName() + (ex.getMessage() != null ? " - " + ex.getMessage() : "");
             }
             alert.setContentText(content);
             alert.showAndWait();
             Platform.exit(); // Attempt graceful exit after showing the alert
         });
     }

     // Helper to show non-critical alerts (like CSS warning) if needed
      private void showAlert(Alert.AlertType type, String title, String message) {
          // Ensure alert is shown on the FX thread
          if (Platform.isFxApplicationThread()) {
              Alert alert = new Alert(type);
              alert.setTitle(title);
              alert.setHeaderText(null);
              alert.setContentText(message);
              alert.showAndWait();
          } else {
              Platform.runLater(() -> {
                  Alert alert = new Alert(type);
                  alert.setTitle(title);
                  alert.setHeaderText(null);
                  alert.setContentText(message);
                  alert.showAndWait();
              });
          }
      }


    /**
     * The main entry point for all JavaFX applications.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Optional: Setup logging configuration here (e.g., file handlers)
        launch(args);
    }

}