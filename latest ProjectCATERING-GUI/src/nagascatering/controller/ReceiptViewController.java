package nagascatering.controller;

import nagascatering.model.Booking;
import nagascatering.model.BookingItem;
import nagascatering.model.Package;

import javafx.application.Platform; // Added for showAlert robustness
import javafx.event.ActionEvent; // Added import
import javafx.fxml.FXML;
import javafx.scene.control.Alert; // Added import
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.format.DateTimeFormatter;
import java.util.List; // Added import
import java.util.logging.Level;

/**
 * Controller for the ReceiptView.fxml.
 * This class needs a corresponding FXML file with elements having the fx:ids used below.
 * It should be displayed (e.g., in a new window or dialog) and populated with data
 * from a selected Booking object.
 */
public class ReceiptViewController implements SubControllerInterface { // Implement if needed

    // --- Assumed FXML elements for ReceiptView.fxml ---
    @FXML private Label receiptTitleLabel; // e.g., "Booking Receipt" or "Quotation"
    @FXML private Label bookingIdLabel;
    @FXML private Label customerNameLabel;
    @FXML private Label customerContactLabel;
    @FXML private Label eventDateLabel;
    @FXML private Label eventTimeLabel;
    @FXML private Label venueAddressLabel;
    @FXML private Label numberOfGuestsLabel;
    @FXML private Label themeLabel;

    @FXML private Label packageNameLabel; // Label for "Package:"
    @FXML private TextFlow packageDetailsTextFlow; // Use TextFlow for multi-line package info

    @FXML private Label itemsTitleLabel; // e.g., "Additional Items" or "Itemized Details"
    @FXML private TextArea itemsTextArea; // To display list of BookingItems

    @FXML private Label customRequestsLabel; // Label for "Notes/Requests:"
    @FXML private TextArea customRequestsTextArea; // Display custom requests

    @FXML private Label baseCostLabel;
    @FXML private Label additionalCostLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label statusLabel;
    // --- End Assumed FXML elements ---

    private MainController mainController; // Optional: if navigation from receipt is needed

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        // Initial setup if needed, e.g., set default text
        // Check if label is injected before setting text to avoid NullPointerException if FXML loading fails partially
        if (receiptTitleLabel != null) {
             receiptTitleLabel.setText("Booking Details / Receipt");
        }
        clearReceipt(); // Clear fields initially
    }

    /**
     * Populates the receipt view with data from the provided Booking object.
     * This method should be called after the FXML is loaded and the controller is initialized.
     *
     * @param booking The Booking object containing the data to display.
     */
    public void loadReceiptData(Booking booking) {
        if (booking == null) {
            showAlert("Error", "Cannot load receipt data: Booking object is null.");
            clearReceipt();
            return;
        }

        // Check if FXML elements are injected before using them
        if (bookingIdLabel == null || customerNameLabel == null /* add other critical labels */) {
            showAlert("Error", "Cannot load receipt data: UI elements not initialized correctly.");
            LOGGER.log(Level.SEVERE, "ReceiptView FXML elements not injected."); // Added logger
            return;
        }


        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy"); // Corrected pattern 'YYYY' to 'yyyy'
        // DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a"); // Example format with AM/PM

        bookingIdLabel.setText(String.valueOf(booking.getBookingId()));
        customerNameLabel.setText(booking.getCustomerName() != null ? booking.getCustomerName() : ""); // Handle potential nulls
        customerContactLabel.setText(booking.getCustomerContact() != null ? booking.getCustomerContact() : "");
        eventDateLabel.setText(booking.getEventDate() != null ? booking.getEventDate().format(dateFormatter) : "N/A");
        eventTimeLabel.setText(booking.getEventTime() != null && !booking.getEventTime().isEmpty() ? booking.getEventTime() : "N/A"); // Assuming HH:mm string
        venueAddressLabel.setText(booking.getVenueAddress() != null ? booking.getVenueAddress() : "N/A");
        numberOfGuestsLabel.setText(String.valueOf(booking.getNumGuests()));
        themeLabel.setText(booking.getThemeDescription() != null && !booking.getThemeDescription().isEmpty() ? booking.getThemeDescription() : "None");

        // --- Package Details ---
        Package selectedPackage = booking.getSelectedPackage(); // Assumes package object is loaded with booking
        // Ensure packageDetailsTextFlow is not null before using
         if (packageDetailsTextFlow != null) {
            packageDetailsTextFlow.getChildren().clear(); // Clear previous content
            if (selectedPackage != null) {
                 packageNameLabel.setText("Package:");
                 Text packageInfo = new Text(
                         (selectedPackage.getName() != null ? selectedPackage.getName() : "Unnamed Package")
                         + " (Capacity: " + selectedPackage.getCapacity() + ")\n" +
                         "Includes: " + (selectedPackage.getIncludedItemsDesc() != null ? selectedPackage.getIncludedItemsDesc() : "See description") + "\n" +
                         "Description: " + (selectedPackage.getDescription() != null ? selectedPackage.getDescription() : "N/A")
                 );
                 packageDetailsTextFlow.getChildren().add(packageInfo);
                 baseCostLabel.setText(String.format("%.2f", booking.getBasePackageCost()));
            } else {
                 packageNameLabel.setText("Package: Custom / None");
                 baseCostLabel.setText(String.format("%.2f", 0.00)); // Base cost is 0 if no package
            }
         } else {
              LOGGER.warning("packageDetailsTextFlow is null. Cannot display package details.");
         }


        // --- Itemized List ---
        List<BookingItem> items = booking.getBookingItems(); // Assumes items are loaded with booking
         // Ensure itemsTextArea is not null
        if (itemsTextArea != null) {
            if (items != null && !items.isEmpty()) {
                itemsTitleLabel.setText("Additional Items:");
                StringBuilder itemsText = new StringBuilder();
                for (BookingItem item : items) {
                    // Use item.toString() or format manually
                     String itemName = (item.getMenuItem() != null && item.getMenuItem().getName() != null) ? item.getMenuItem().getName() : "Item ID: " + item.getItemId();
                     itemsText.append(String.format(" • %s (Qty: %d) - Cost: %.2f\n",
                                                    itemName,
                                                    item.getQuantity(),
                                                    item.getCalculatedItemCost()));
                }
                itemsTextArea.setText(itemsText.toString());
                additionalCostLabel.setText(String.format("%.2f", booking.getAdditionalItemsCost()));
            } else {
                 itemsTitleLabel.setText("Additional Items: None");
                 itemsTextArea.clear();
                 additionalCostLabel.setText(String.format("%.2f", 0.00));
            }
        } else {
             LOGGER.warning("itemsTextArea is null. Cannot display itemized list.");
        }


        // --- Custom Requests / Notes ---
        // Ensure customRequestsTextArea is not null
        if (customRequestsTextArea != null) {
             customRequestsLabel.setText("Notes / Custom Requests:");
             customRequestsTextArea.setText(booking.getCustomRequests() != null && !booking.getCustomRequests().isEmpty() ? booking.getCustomRequests() : "None");
        } else {
            LOGGER.warning("customRequestsTextArea is null. Cannot display custom requests.");
        }


        // --- Totals and Status ---
        totalCostLabel.setText(String.format("%.2f", booking.getTotalCost()));
        statusLabel.setText(booking.getBookingStatus() != null ? booking.getBookingStatus() : "N/A");
    }

    // Clears all fields in the receipt view
    private void clearReceipt() {
        // Add null checks for safety during initialization or if FXML fails
        if(bookingIdLabel != null) bookingIdLabel.setText("-");
        if(customerNameLabel != null) customerNameLabel.setText("-");
        if(customerContactLabel != null) customerContactLabel.setText("-");
        if(eventDateLabel != null) eventDateLabel.setText("-");
        if(eventTimeLabel != null) eventTimeLabel.setText("-");
        if(venueAddressLabel != null) venueAddressLabel.setText("-");
        if(numberOfGuestsLabel != null) numberOfGuestsLabel.setText("-");
        if(themeLabel != null) themeLabel.setText("-");
        if(packageNameLabel != null) packageNameLabel.setText("Package:");
        if(packageDetailsTextFlow != null) packageDetailsTextFlow.getChildren().clear();
        if(itemsTitleLabel != null) itemsTitleLabel.setText("Additional Items:");
        if(itemsTextArea != null) itemsTextArea.clear();
        if(customRequestsLabel != null) customRequestsLabel.setText("Notes / Custom Requests:");
        if(customRequestsTextArea != null) customRequestsTextArea.clear();
        if(baseCostLabel != null) baseCostLabel.setText("0.00");
        if(additionalCostLabel != null) additionalCostLabel.setText("0.00");
        if(totalCostLabel != null) totalCostLabel.setText("0.00");
        if(statusLabel != null) statusLabel.setText("-");
    }

    // --- Optional Actions ---
    @FXML
    private void handlePrintReceipt(ActionEvent event) {
        // TODO: Implement printing logic using JavaFX printing API
        // This involves creating a Printable Node (e.g., the root pane of the receipt)
        // and using PrinterJob.
        showAlert("Info", "Print functionality not yet implemented.");
    }

    @FXML
    private void handleCloseReceipt(ActionEvent event) {
        // Close the window/stage containing this receipt view
        if (receiptTitleLabel != null && receiptTitleLabel.getScene() != null && receiptTitleLabel.getScene().getWindow() != null) {
            receiptTitleLabel.getScene().getWindow().hide(); // Or .close() if it's the primary stage (e.g., Stage.close())
        } else {
             // Log the issue, alert might not be necessary if window is already gone or never appeared
              LOGGER.warning("Could not close the receipt window - scene or window not found.");
             // showAlert("Error", "Could not close the receipt window.");
        }
    }


     // Utility method to show alerts - runs on FX thread
    private void showAlert(String title, String message) {
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

     // Added Logger instance
     private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(ReceiptViewController.class.getName());

}