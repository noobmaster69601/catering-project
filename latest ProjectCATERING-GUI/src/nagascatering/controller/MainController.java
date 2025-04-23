package nagascatering.controller;

import java.io.IOException;
import java.net.URL; // Import URL for checking resource existence
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform; // Import Platform for showAlert
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert; // Import Alert
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class MainController {

    @FXML
    private BorderPane contentArea; // The central area of MainView.fxml

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    private void initialize() {
        // Load the dashboard initially after UI setup is complete
        Platform.runLater(() -> showDashboard(null));
    }

    @FXML
    void handleClose(ActionEvent event) {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        stage.close();
    }

    @FXML
    void showDashboard(ActionEvent event) {
        // Use absolute path starting with '/'
        loadView("/nagascatering/view/Dashboard.fxml", null); // Pass null data
    }

    @FXML
    void showBookingForm(ActionEvent event) {
        // Use absolute path starting with '/'
        loadView("/nagascatering/view/BookingForm.fxml", null); // Pass null data
    }

     @FXML
    void showBookingList(ActionEvent event) {
         // Example: Navigate to a booking list view
         loadView("/nagascatering/view/BookingListView.fxml", null); // Adjust path as needed
    }


    @FXML
    void showPackageManager(ActionEvent event) {
        // Use absolute path starting with '/'
        loadView("/nagascatering/view/PackageManager.fxml", null); // Pass null data
    }

     @FXML
    void showMenuItemManager(ActionEvent event) {
        // Example: Navigate to a menu item manager view
        loadView("/nagascatering/view/MenuItemManager.fxml", null); // Adjust path as needed
    }


    // --- Helper method to load views into the content area ---
    // Updated to handle data passing (though data is null in direct calls above)
    private <T> T loadView(String fxmlPath, Object data) {
        T controllerInstance = null;
        try {
            // Check if resource exists first
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find FXML resource: " + fxmlPath + ". Check the path and file existence.");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent view = loader.load();

            // --- Setup Controller Communication & Data Passing ---
            Object controller = loader.getController();
            controllerInstance = (T) controller; // Cast to generic type

            if (controller instanceof SubControllerInterface) {
                ((SubControllerInterface) controller).setMainController(this);
                LOGGER.info("Passed MainController reference to: " + controller.getClass().getSimpleName());

                // Example: Call initializeData if the interface defines it and data is provided
                // This requires the SubControllerInterface to have the initializeData method
                // and the specific controller to implement it safely (checking data type).
                /*
                if (data != null && controller instanceof SubControllerInterface) {
                    try {
                         // Assuming SubControllerInterface has initializeData(Object data)
                         // ((SubControllerInterface) controller).initializeData(data);
                         LOGGER.info("Passed data ("+ data.getClass().getSimpleName() +") to: " + controller.getClass().getSimpleName());
                    } catch (Exception e) {
                         LOGGER.log(Level.SEVERE, "Error calling initializeData on " + controller.getClass().getSimpleName(), e);
                    }
                }
                */

            } else if (controller != null) {
                 LOGGER.warning("Controller " + controller.getClass().getName() + " does not implement SubControllerInterface");
            } else {
                LOGGER.warning("No controller found for FXML: " + fxmlPath);
            }
            // --- End Setup ---

            contentArea.setCenter(view);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load view: " + fxmlPath, e);
            showAlert("Load Error", "Could not load the screen: " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1) + "\nReason: " + e.getMessage());
        } catch (ClassCastException e) {
            LOGGER.log(Level.SEVERE, "Controller type mismatch for view: " + fxmlPath, e);
            showAlert("Load Error", "Controller type mismatch for screen: " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1));
        } catch (Exception e) { // Catch any other unexpected errors during loading
            LOGGER.log(Level.SEVERE, "Unexpected error loading view: " + fxmlPath, e);
            showAlert("Load Error", "An unexpected error occurred while loading:\n" + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1));
        }
        return controllerInstance; // Return the controller instance (or null on error)
    }

    // --- Implemented showAlert method ---
    private void showAlert(String title, String message) {
         // Ensure UI updates happen on the JavaFX Application Thread
         if (Platform.isFxApplicationThread()) {
            showActualAlert(title, message);
         } else {
              Platform.runLater(() -> showActualAlert(title, message));
         }
    }

     private void showActualAlert(String title, String message){
         Alert.AlertType type = title.toLowerCase().contains("error") ? Alert.AlertType.ERROR :
                               (title.toLowerCase().contains("warning") ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
         Alert alert = new Alert(type);
         alert.setTitle(title);
         alert.setHeaderText(null); // No header text for simplicity
         alert.setContentText(message);
         alert.showAndWait();
     }


    // --- Navigation methods accessible by sub-controllers ---

    /**
     * Loads the specified FXML view into the main content area.
     * Ensures the path starts with '/' for absolute loading from classpath root.
     * @param fxmlPath The absolute path to the FXML file (e.g., "/nagascatering/view/Dashboard.fxml")
     */
     public void navigateTo(String fxmlPath) {
         // Ensure path is absolute for consistency
         String absolutePath = fxmlPath.startsWith("/") ? fxmlPath : "/" + fxmlPath;
         loadView(absolutePath, null); // Call loadView with null data
     }

    /**
     * Loads the specified FXML view, passes data to its controller (if applicable),
     * and returns the controller instance.
     * Ensures the path starts with '/' for absolute loading from classpath root.
     * @param fxmlPath The absolute path to the FXML file (e.g., "/nagascatering/view/BookingForm.fxml")
     * @param data Optional data object to pass to the controller after initialization.
     * @return The controller instance of the loaded view, or null if loading fails.
     */
     public <T> T navigateTo(String fxmlPath, Object data) {
        // Ensure path is absolute for consistency
        String absolutePath = fxmlPath.startsWith("/") ? fxmlPath : "/" + fxmlPath;
        // Call the updated loadView method which handles data passing logic
        return loadView(absolutePath, data);
     }
}