package nagascatering.controller;

import nagascatering.data.InMemoryDataManager; // Ensure this is the correct data manager
import nagascatering.model.Booking;
import nagascatering.model.Package;
import nagascatering.model.BookingItem;
import nagascatering.model.MenuItem;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public class BookingFormController implements Initializable, SubControllerInterface {

    // --- FXML fields ---
    @FXML private TextField customerNameField;
    @FXML private TextField customerContactField;
    @FXML private DatePicker eventDatePicker;
    @FXML private TextField eventTimeField; // Expects HH:mm format or empty
    @FXML private Spinner<Integer> numGuestsSpinner;
    @FXML private TextArea venueAddressArea;
    @FXML private TextField themeField;
    @FXML private ComboBox<Package> packageComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextArea customRequestsArea; // General requests
    @FXML private Label costLabel;
    @FXML private ProgressIndicator progressIndicator; // Added FXML annotation
    @FXML private Button saveButton; // Added FXML annotation
    @FXML private Button clearButton; // Added FXML annotation

    // Custom items elements
    @FXML private ComboBox<MenuItem> menuItemComboBox;
    @FXML private Spinner<Integer> itemQuantitySpinner;
    @FXML private ListView<BookingItem> bookingItemsListView;
    @FXML private Button addItemButton;
    @FXML private Button removeItemButton;

    private InMemoryDataManager dataManager;
    private ObservableList<Package> packageList = FXCollections.observableArrayList();
    private MainController mainController;

    private ObservableList<BookingItem> currentBookingItems = FXCollections.observableArrayList();
    private ObservableList<MenuItem> availableMenuItems = FXCollections.observableArrayList();

    private static final Logger LOGGER = Logger.getLogger(BookingFormController.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize DataManager (Consider dependency injection later)
        dataManager = new InMemoryDataManager();
        progressIndicator.setVisible(false); // Ensure hidden at start
        setupSpinners();
        setupPackageComboBox();
        loadStatusOptions();
        setupCustomItemControls();
        loadInitialData(); // Load packages and menu items asynchronously
        clearForm(); // Set initial state
    }

    private void setupSpinners() {
        SpinnerValueFactory<Integer> guestValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 50);
        numGuestsSpinner.setValueFactory(guestValueFactory);
        numGuestsSpinner.setEditable(true);
        // Recalculate cost whenever guest count changes
        numGuestsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> handleCalculateCost(null));
    }

    private void loadInitialData() {
        progressIndicator.setVisible(true);
        setControlsDisabled(true);

        Task<List<Package>> loadPackagesTask = createLoadPackagesTask();
        Task<List<MenuItem>> loadMenuItemsTask = createLoadMenuItemsTask();

        loadPackagesTask.setOnSucceeded(e -> {
            packageList.setAll(loadPackagesTask.getValue());
            checkDataLoadCompletion(loadMenuItemsTask); // Check if the other task is done
        });
        loadPackagesTask.setOnFailed(e -> handleDataLoadFailure("packages", loadPackagesTask.getException(), loadMenuItemsTask));

        loadMenuItemsTask.setOnSucceeded(e -> {
            availableMenuItems.setAll(loadMenuItemsTask.getValue());
             menuItemComboBox.setItems(availableMenuItems); // Ensure combo box gets updated items
            checkDataLoadCompletion(loadPackagesTask); // Check if the other task is done
        });
        loadMenuItemsTask.setOnFailed(e -> handleDataLoadFailure("menu items", loadMenuItemsTask.getException(), loadPackagesTask));

        // Start tasks in separate threads
        new Thread(loadPackagesTask).start();
        new Thread(loadMenuItemsTask).start();
    }

    private Task<List<Package>> createLoadPackagesTask() {
        return new Task<List<Package>>() {
            @Override
            protected List<Package> call() throws Exception {
                // Assuming getAllPackages(false) gets only active packages
                return dataManager.getAllPackages(false);
            }
        };
    }

    private Task<List<MenuItem>> createLoadMenuItemsTask() {
        return new Task<List<MenuItem>>() {
            @Override
            protected List<MenuItem> call() throws Exception {
                 // Assuming getAllMenuItems() gets active items by default or handles filtering
                return dataManager.getAllMenuItems();
            }
        };
    }

    // Checks if both loading tasks are complete before re-enabling UI
    private void checkDataLoadCompletion(Task<?> otherTask) {
        if (otherTask.isDone()) {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                setControlsDisabled(false);
                clearForm(); // Re-apply default state after loading
                LOGGER.info("Initial data loaded successfully.");
            });
        }
    }

    // Handles failure during data loading
    private void handleDataLoadFailure(String dataType, Throwable error, Task<?> otherTask) {
        LOGGER.log(Level.SEVERE, "Failed to load " + dataType, error);
        Platform.runLater(() -> {
            showAlert("Error Loading Data", "Could not load " + dataType + ". Please check connection or data source.\nError: " + error.getMessage());
            // Still try to re-enable UI even if one part failed
            checkDataLoadCompletion(otherTask);
        });
    }

    private void setupPackageComboBox() {
        packageComboBox.setItems(packageList);
        packageComboBox.setConverter(new StringConverter<Package>() {
            @Override
            public String toString(Package object) {
                // Provide a clear default text when no package is selected
                return object == null ? "--- No Package Selected ---" : object.toString();
            }
            @Override
            public Package fromString(String string) {
                 // Not needed for selection only
                return null;
            }
        });
        packageComboBox.getSelectionModel().clearSelection(); // Start with nothing selected
        // Listener moved to FXML onAction="#handleCalculateCost" for simplicity
        // packageComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> handleCalculateCost(null));
    }

    private void loadStatusOptions() {
        statusComboBox.setItems(FXCollections.observableArrayList(
            "Pending", "Confirmed", "Completed", "Cancelled"
        ));
        statusComboBox.getSelectionModel().select("Pending"); // Default status
    }

    private void setupCustomItemControls() {
        // Defensive check for FXML injection
        if (menuItemComboBox == null || itemQuantitySpinner == null || bookingItemsListView == null || addItemButton == null || removeItemButton == null) {
            LOGGER.severe("FXML elements for custom items section were not injected!");
            showAlert("Initialization Error", "Critical UI components for adding items are missing. Cannot proceed reliably.");
            // Optionally disable the entire section or application
            return;
        }

        // menuItemComboBox is populated after data load, set converter here
        menuItemComboBox.setConverter(new StringConverter<MenuItem>() {
            @Override
            public String toString(MenuItem item) {
                return item == null ? "Select Item..." : item.toString(); // item.toString() should be descriptive
            }
            @Override
            public MenuItem fromString(String string) { return null; } // Not needed
        });
        menuItemComboBox.getSelectionModel().clearSelection(); // Start empty

        SpinnerValueFactory<Integer> itemQtyFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        itemQuantitySpinner.setValueFactory(itemQtyFactory);
        itemQuantitySpinner.setEditable(true);

        bookingItemsListView.setItems(currentBookingItems);
        bookingItemsListView.setPlaceholder(new Label("No additional items added."));
        // Customize cell display
        bookingItemsListView.setCellFactory(lv -> new ListCell<BookingItem>() {
            @Override
            protected void updateItem(BookingItem item, boolean empty) {
                super.updateItem(item, empty);
                // Ensure BookingItem.toString() provides a good representation
                setText((empty || item == null) ? null : item.toString());
            }
        });

        // Enable/Disable Remove button based on list selection
        removeItemButton.setDisable(true); // Start disabled
        bookingItemsListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSel, newSel) -> removeItemButton.setDisable(newSel == null)
        );

        // Enable/Disable Add button based on MenuItem selection
        addItemButton.setDisable(true); // Start disabled
        menuItemComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSel, newSel) -> addItemButton.setDisable(newSel == null)
        );
    }

    @FXML
    void handleCalculateCost(ActionEvent event) {
        Package selectedPackage = packageComboBox.getValue();
        double baseCost = (selectedPackage != null) ? selectedPackage.getPrice() : 0.0;

        // Must recalculate per-person items FIRST, as guest count might have changed
        recalculatePerPersonItemCosts();

        // Then calculate total cost of additional items
        double additionalCost = calculateAdditionalItemsCost();

        double totalCost = baseCost + additionalCost;
        costLabel.setText(String.format("₱ %.2f", totalCost));
    }

    // Recalculates cost for items marked 'per_person' based on current guest count
    private void recalculatePerPersonItemCosts() {
        int currentGuests = numGuestsSpinner.getValue() != null ? numGuestsSpinner.getValue() : 0;
        boolean costChanged = false;

        for (BookingItem item : currentBookingItems) {
            MenuItem menuItem = item.getMenuItem(); // Get the associated MenuItem
             // Ensure menuItem is loaded and type is 'per_person' (case-insensitive)
            if (menuItem != null && "per_person".equalsIgnoreCase(menuItem.getUnitType())) {
                 // Cost is per unit * number of guests
                double newCost = menuItem.getCostPerUnit() * currentGuests;
                 // Use Double.compare for safe floating-point comparison
                if (Double.compare(newCost, item.getCalculatedItemCost()) != 0) {
                    item.setCalculatedItemCost(newCost);
                    costChanged = true; // Mark that a change occurred
                }
            }
        }

        // If any per-person item cost changed, refresh the list view and update total cost display
        if (costChanged) {
            bookingItemsListView.refresh(); // Update display of items in the list
           // Don't call handleCalculateCost from here to avoid potential infinite loop
           // Just update the total cost label directly after recalculating all items
            double baseCost = (packageComboBox.getValue() != null) ? packageComboBox.getValue().getPrice() : 0.0;
            double additionalCost = calculateAdditionalItemsCost(); // Get sum of updated item costs
            costLabel.setText(String.format("₱ %.2f", baseCost + additionalCost));
        }
    }


    // Calculates the sum of costs for all items in the currentBookingItems list
    private double calculateAdditionalItemsCost() {
         // Ensure per-person items are updated before summing
        // recalculatePerPersonItemCosts(); // Careful: This might be redundant if called just before
        return currentBookingItems.stream()
               .mapToDouble(BookingItem::getCalculatedItemCost) // Use the calculated cost
               .sum();
    }

    @FXML
    void handleAddItem(ActionEvent event) {
        MenuItem selectedMenuItem = menuItemComboBox.getValue();
        if (selectedMenuItem == null) {
            showAlert("Selection Error", "Please select a menu item from the dropdown to add.");
            return;
        }

        int quantity;
        try {
            // Make sure spinner value is valid
            quantity = itemQuantitySpinner.getValue();
            if (quantity <= 0) {
                showAlert("Input Error", "Quantity must be a positive number (at least 1).");
                return;
            }
        } catch (NullPointerException | NumberFormatException e) {
             // Catch potential errors if spinner text is invalid
            showAlert("Input Error", "Invalid quantity specified. Please enter a valid number.");
            return;
        }

        // Prevent adding the *same* non-per-person item multiple times.
        // Allow multiple additions if it's a different item or 'per_person' (as cost depends on guests).
        for (BookingItem existingItem : currentBookingItems) {
            // Check if item ID matches AND it's NOT a per-person item
            if (existingItem.getItemId() == selectedMenuItem.getItemId() &&
                !"per_person".equalsIgnoreCase(selectedMenuItem.getUnitType())) {
                showAlert("Item Already Added", selectedMenuItem.getName() + " (non-per-person) is already in the list.\nRemove it first if you need to change its quantity.");
                return;
            }
        }

        // Create and configure the new BookingItem
        BookingItem newItem = new BookingItem();
        newItem.setItemId(selectedMenuItem.getItemId());
        newItem.setMenuItem(selectedMenuItem); // Store reference to MenuItem for details
        newItem.setQuantity(quantity);
        // Calculate cost based on type and quantity/guests NOW
        newItem.setCalculatedItemCost(calculateItemLineCost(selectedMenuItem, quantity));

        currentBookingItems.add(newItem); // Add to the observable list (updates ListView)
        handleCalculateCost(null); // Update the total cost display

        // Reset input fields for the next item
        menuItemComboBox.getSelectionModel().clearSelection();
        itemQuantitySpinner.getValueFactory().setValue(1); // Reset quantity to default
    }

    @FXML
    void handleRemoveItem(ActionEvent event) {
        BookingItem selectedBookingItem = bookingItemsListView.getSelectionModel().getSelectedItem();
        if (selectedBookingItem == null) {
            showAlert("Selection Error", "Please select an item from the 'Additional Items' list to remove.");
            return;
        }
        currentBookingItems.remove(selectedBookingItem); // Remove from the list
        handleCalculateCost(null); // Update the total cost display
    }

    // Calculates the cost for a single line item based on its type
    private double calculateItemLineCost(MenuItem menuItem, int quantity) {
        if (menuItem == null || quantity <= 0) return 0.0;

        int currentGuests = numGuestsSpinner.getValue() != null ? numGuestsSpinner.getValue() : 0;

        if ("per_person".equalsIgnoreCase(menuItem.getUnitType())) {
             // Cost is per unit * number of guests
            return menuItem.getCostPerUnit() * currentGuests;
        } else {
            // Cost is per unit * quantity specified
            return menuItem.getCostPerUnit() * quantity;
        }
    }

    @FXML
    void handleSaveBooking(ActionEvent event) {
        if (!validateInput()) {
             // Validation failed, message already shown
            return;
        }

        // Create Booking object and populate from form fields
        Booking booking = new Booking();
        booking.setCustomerName(customerNameField.getText().trim());
        booking.setCustomerContact(customerContactField.getText().trim());
        booking.setEventDate(eventDatePicker.getValue());
        // Handle potentially empty time field gracefully
        String timeText = eventTimeField.getText().trim();
        booking.setEventTime(timeText.isEmpty() ? null : timeText);
        booking.setVenueAddress(venueAddressArea.getText().trim());
        booking.setThemeDescription(themeField.getText().trim());
        booking.setNumGuests(numGuestsSpinner.getValue());
        booking.setBookingStatus(statusComboBox.getValue());
        booking.setCustomRequests(customRequestsArea.getText().trim()); // General requests

        // Handle selected package
        Package selectedPackage = packageComboBox.getValue();
        double baseCost = 0.0;
        if (selectedPackage != null) {
            booking.setSelectedPackageId(selectedPackage.getPackageId());
            baseCost = selectedPackage.getPrice();
        } else {
            booking.setSelectedPackageId(null); // Explicitly null if no package
        }

        // Final cost calculation before saving
        recalculatePerPersonItemCosts(); // Ensure per-person items are correct
        double additionalCost = calculateAdditionalItemsCost(); // Get final sum of added items
        booking.setBasePackageCost(baseCost);
        booking.setAdditionalItemsCost(additionalCost);
        booking.setTotalCost(baseCost + additionalCost); // Sum of package + items

        // Prepare list of items to be saved with the booking
        // Create a new list to avoid modification issues if save is async
        List<BookingItem> itemsToSave = new ArrayList<>(currentBookingItems);

        // Disable buttons and show progress indicator during save operation
        saveButton.setDisable(true);
        clearButton.setDisable(true);
        setControlsDisabled(true); // Disable form fields too
        progressIndicator.setVisible(true);

        // Make final copies for use in the background task lambda
        final Booking finalBooking = booking;
        final List<BookingItem> finalItemsToSave = itemsToSave;

        // Create and run the save task in a background thread
        Task<Boolean> saveTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Call the data manager to save the booking and its associated items
                // This should handle assigning a booking ID to finalBooking if new
                return dataManager.saveBooking(finalBooking, finalItemsToSave);
            }
        };

        // Handle task success (on JavaFX Application Thread)
        saveTask.setOnSucceeded(e -> Platform.runLater(() -> {
            boolean success = saveTask.getValue(); // Get result from call()
            progressIndicator.setVisible(false); // Hide indicator
            // Re-enable buttons regardless of success/failure, but not necessarily form fields yet
            saveButton.setDisable(false);
            clearButton.setDisable(false);
            // Only re-enable form fields if staying on the form makes sense
            // setControlsDisabled(false); // Re-enable form fields

            if (success) {
                showAlert("Success", "Booking saved successfully! Booking ID: " + finalBooking.getBookingId());
                clearForm(); // Clear form for a new booking entry
                setControlsDisabled(false); // Re-enable form fields for the now cleared form
            } else {
                showAlert("Save Error", "Failed to save the booking. Please check application logs or data store connection.");
                setControlsDisabled(false); // Re-enable form fields so user can try again
            }
        }));

        // Handle task failure (on JavaFX Application Thread)
        saveTask.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = saveTask.getException();
            LOGGER.log(Level.SEVERE, "Error saving booking", error);
            progressIndicator.setVisible(false); // Hide indicator
            saveButton.setDisable(false); // Re-enable buttons
            clearButton.setDisable(false);
            setControlsDisabled(false); // Re-enable form fields
            showAlert("Application Error", "An unexpected error occurred while saving the booking:\n" + error.getMessage());
        }));

        // Start the background task
        new Thread(saveTask).start();
    }

    // Validates required input fields and provides user feedback
    private boolean validateInput() {
        boolean isValid = true;
        clearValidationStyles(); // Remove previous error styles

        // Use a StringBuilder for a consolidated error message (optional)
        // StringBuilder errors = new StringBuilder();

        if (customerNameField.getText() == null || customerNameField.getText().trim().isEmpty()) {
            customerNameField.getStyleClass().add("text-field-error"); isValid = false;
            // errors.append("- Customer Name is required.\n");
        }
        if (customerContactField.getText() == null || customerContactField.getText().trim().isEmpty()) {
            customerContactField.getStyleClass().add("text-field-error"); isValid = false;
            // errors.append("- Contact Number is required.\n");
        } else if (!customerContactField.getText().trim().matches("^[\\d\\s+-]{5,}$")) {
             // Basic format check (digits, spaces, plus, hyphen, min length 5)
            customerContactField.getStyleClass().add("text-field-error"); isValid = false;
            showAlert("Validation Error", "Contact Number format seems invalid. Use digits, spaces, +, -.");
             // errors.append("- Contact Number format is invalid.\n");
        }

        if (eventDatePicker.getValue() == null) {
            eventDatePicker.getEditor().getStyleClass().add("text-field-error"); // Style the editor part
            isValid = false;
            // errors.append("- Event Date is required.\n");
        } else if (eventDatePicker.getValue().isBefore(LocalDate.now())) {
            eventDatePicker.getEditor().getStyleClass().add("text-field-error");
            showAlert("Validation Error", "Event date cannot be in the past.");
            isValid = false;
            // errors.append("- Event Date cannot be in the past.\n");
        }

        // Validate time only if provided
        String timeText = eventTimeField.getText().trim();
        if (!timeText.isEmpty()) {
            try {
                LocalTime.parse(timeText, TIME_FORMATTER); // Try parsing HH:mm
            } catch (DateTimeParseException ex) {
                eventTimeField.getStyleClass().add("text-field-error");
                showAlert("Validation Error", "Event time, if entered, must be in HH:mm format (e.g., 14:30).");
                isValid = false;
                // errors.append("- Event Time format is invalid (must be HH:mm or empty).\n");
            }
        }

        // Validate guest spinner
        try {
            if (numGuestsSpinner.getValue() == null || numGuestsSpinner.getValue() <= 0) {
                numGuestsSpinner.getEditor().getStyleClass().add("text-field-error"); isValid = false;
                 // errors.append("- Number of Guests must be greater than 0.\n");
            }
        } catch (Exception e) { // Catch potential exceptions if editor text is invalid
            numGuestsSpinner.getEditor().getStyleClass().add("text-field-error"); isValid = false;
             // errors.append("- Invalid input for Number of Guests.\n");
        }

        if (venueAddressArea.getText() == null || venueAddressArea.getText().trim().isEmpty()) {
            venueAddressArea.getStyleClass().add("text-field-error"); isValid = false;
             // errors.append("- Venue Address is required.\n");
        }

        // Show a single summary message if any errors occurred
        if (!isValid) {
            showAlert("Validation Error", "Please correct the highlighted fields before saving.");
            // showAlert("Validation Errors", "Please fix the following issues:\n" + errors.toString());
        }

        return isValid;
    }

    // Removes the error styling from input fields
    private void clearValidationStyles() {
        customerNameField.getStyleClass().remove("text-field-error");
        customerContactField.getStyleClass().remove("text-field-error");
        eventDatePicker.getStyleClass().remove("text-field-error"); // Target DatePicker itself
        eventDatePicker.getEditor().getStyleClass().remove("text-field-error"); // Target editor field inside
        eventTimeField.getStyleClass().remove("text-field-error");
        numGuestsSpinner.getEditor().getStyleClass().remove("text-field-error"); // Target editor field inside
        venueAddressArea.getStyleClass().remove("text-field-error");
    }

    @FXML
    void handleClearForm(ActionEvent event) {
        clearForm();
    }

    // Resets the form to its initial state
    private void clearForm() {
        customerNameField.clear();
        customerContactField.clear();
        eventDatePicker.setValue(null);
        eventTimeField.clear();
        // Reset spinner to default value defined in setupSpinners
        if (numGuestsSpinner.getValueFactory() != null) {
             numGuestsSpinner.getValueFactory().setValue(50); // Or get initial value if stored
        }
        venueAddressArea.clear();
        themeField.clear();
        packageComboBox.getSelectionModel().clearSelection();
        statusComboBox.getSelectionModel().select("Pending"); // Default status
        customRequestsArea.clear();
        costLabel.setText("₱ 0.00"); // Reset cost display

        // Clear custom items section
        currentBookingItems.clear(); // Clears the list view via binding
        menuItemComboBox.getSelectionModel().clearSelection();
        if(itemQuantitySpinner.getValueFactory() != null) {
            itemQuantitySpinner.getValueFactory().setValue(1); // Reset quantity
        }
        addItemButton.setDisable(true); // Should be disabled as no item selected
        removeItemButton.setDisable(true); // Should be disabled as no list item selected


        clearValidationStyles(); // Remove any lingering error styles

        // Ensure main buttons are enabled and progress is hidden
        saveButton.setDisable(false);
        clearButton.setDisable(false);
        progressIndicator.setVisible(false);

        // Optionally re-enable all controls if they were disabled
        // setControlsDisabled(false);

        customerNameField.requestFocus(); // Set focus to the first field
    }

    // Utility to enable/disable all interactive controls, e.g., during loading/saving
    private void setControlsDisabled(boolean disabled) {
        // Basic Info
        customerNameField.setDisable(disabled);
        customerContactField.setDisable(disabled);
        eventDatePicker.setDisable(disabled);
        eventTimeField.setDisable(disabled);
        numGuestsSpinner.setDisable(disabled);
        venueAddressArea.setDisable(disabled);
        themeField.setDisable(disabled);
        packageComboBox.setDisable(disabled);
        statusComboBox.setDisable(disabled);
        customRequestsArea.setDisable(disabled);

        // Custom Items Section
        menuItemComboBox.setDisable(disabled);
        itemQuantitySpinner.setDisable(disabled);
        bookingItemsListView.setDisable(disabled); // Disable the list itself
        // Buttons need special handling based on state even when enabled
        addItemButton.setDisable(disabled || menuItemComboBox.getValue() == null);
        removeItemButton.setDisable(disabled || bookingItemsListView.getSelectionModel().getSelectedItem() == null);

        // Main Action Buttons (often handled separately, but included here for completeness)
        saveButton.setDisable(disabled);
        clearButton.setDisable(disabled);
    }

    // Helper method for showing alerts, ensures runs on FX thread
    private void showAlert(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            showActualAlert(title, message);
        } else {
            Platform.runLater(() -> showActualAlert(title, message));
        }
    }

    // Creates and shows the actual Alert dialog
    private void showActualAlert(String title, String message) {
        Alert.AlertType type;
        if (title.toLowerCase().contains("error")) {
            type = Alert.AlertType.ERROR;
        } else if (title.toLowerCase().contains("warning")) {
            type = Alert.AlertType.WARNING;
        } else {
            type = Alert.AlertType.INFORMATION; // Default to Information
        }
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header text, direct message
        alert.setContentText(message);
        alert.showAndWait();
    }
}