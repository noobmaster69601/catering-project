package nagascatering.controller;

import nagascatering.data.InMemoryDataManager;
import nagascatering.model.Package;

import java.net.URL;
import java.util.List;
import java.util.Optional;
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
import javafx.scene.control.cell.PropertyValueFactory;

public class PackageManagerController implements Initializable, SubControllerInterface {

    @FXML private TableView<Package> packageTableView;
    @FXML private TableColumn<Package, String> colPackageName;
    @FXML private TableColumn<Package, Double> colPrice;
    @FXML private TableColumn<Package, Integer> colCapacity;
    @FXML private TableColumn<Package, Boolean> colIsActive;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField capacityField;
    @FXML private CheckBox isActiveCheckBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea includedItemsArea;
    @FXML private CheckBox showInactiveCheckBox;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button newButton;
    @FXML private Button clearButton;

    private InMemoryDataManager dataManager;
    private ObservableList<Package> packageList = FXCollections.observableArrayList();
    private Package currentlySelectedPackage = null;
    private MainController mainController;

    private static final Logger LOGGER = Logger.getLogger(PackageManagerController.class.getName());

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = new InMemoryDataManager();
        progressIndicator.setVisible(false);
        configureTable();
        loadPackages();

        showInactiveCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> loadPackages());
        clearPackageForm();
        setFormEditable(false);
        deleteButton.setDisable(true);
    }

    private void configureTable() {
        packageTableView.setItems(packageList);
        packageTableView.setPlaceholder(new Label("No packages found. Create one using 'New'."));

        colPackageName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colIsActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        colPrice.setCellFactory(tc -> new TableCell<Package, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : String.format("₱%.2f", price));
            }
        });

        colIsActive.setCellFactory(tc -> new TableCell<Package, Boolean>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    getStyleClass().removeAll("status-active", "status-inactive");
                } else {
                    setText(active ? "Active" : "Inactive");
                    getStyleClass().removeAll("status-active", "status-inactive");
                    getStyleClass().add(active ? "status-active" : "status-inactive");
                }
            }
        });

        packageTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    populateForm(newValue);
                    setFormEditable(newValue != null);
                    deleteButton.setDisable(newValue == null);
                });
    }

    @FXML
    void handleRefreshTable(ActionEvent event) {
        loadPackages();
    }

    private void loadPackages() {
        progressIndicator.setVisible(true);
        setControlsDisabled(true);

        Task<List<Package>> loadTask = new Task<List<Package>>() {
            @Override
            protected List<Package> call() throws Exception {
                boolean includeInactive = showInactiveCheckBox.isSelected();
                return dataManager.getAllPackages(includeInactive);
            }
        };

        loadTask.setOnSucceeded(e -> Platform.runLater(() -> {
            packageList.setAll(loadTask.getValue());
            clearPackageForm();
            progressIndicator.setVisible(false);
            setControlsDisabled(false);
            setFormEditable(false);
            deleteButton.setDisable(true);
        }));

        loadTask.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = loadTask.getException();
            LOGGER.log(Level.SEVERE, "Failed to load packages", error);
            showAlert("Error", "Could not load packages: " + error.getMessage());
            packageList.clear();
            clearPackageForm();
            progressIndicator.setVisible(false);
            setControlsDisabled(false);
            setFormEditable(false);
            deleteButton.setDisable(true);
        }));

        new Thread(loadTask).start();
    }

    private void populateForm(Package pkg) {
        currentlySelectedPackage = pkg;
        boolean isPkgSelected = (pkg != null);

        nameField.setText(isPkgSelected ? pkg.getName() : "");
        priceField.setText(isPkgSelected ? String.format("%.2f", pkg.getPrice()) : "");
        capacityField.setText(isPkgSelected ? String.valueOf(pkg.getCapacity()) : "");
        isActiveCheckBox.setSelected(isPkgSelected ? pkg.isActive() : true);
        descriptionArea.setText(isPkgSelected ? pkg.getDescription() : "");
        includedItemsArea.setText(isPkgSelected ? pkg.getIncludedItemsDesc() : "");

        clearValidationStyles();

        if (isPkgSelected) {
            Platform.runLater(() -> nameField.requestFocus());
        }
    }

    @FXML
    void handleNewPackage(ActionEvent event) {
        packageTableView.getSelectionModel().clearSelection();
        clearPackageForm();
        currentlySelectedPackage = null;
        setFormEditable(true);
        isActiveCheckBox.setSelected(true);
        deleteButton.setDisable(true);
        nameField.requestFocus();
    }

    @FXML
    void handleSavePackage(ActionEvent event) {
        if (!validatePackageForm()) {
            return;
        }

        boolean isNew = (currentlySelectedPackage == null || currentlySelectedPackage.getPackageId() <= 0);
        Package pkgToSave;

        if (isNew) {
            pkgToSave = new Package();
        } else {
            pkgToSave = currentlySelectedPackage;
            if (pkgToSave == null) {
                showAlert("Error", "Cannot save, selected package is no longer valid.");
                return;
            }
        }

        try {
            pkgToSave.setName(nameField.getText().trim());
            pkgToSave.setPrice(Double.parseDouble(priceField.getText().trim()));
            pkgToSave.setCapacity(Integer.parseInt(capacityField.getText().trim()));
            pkgToSave.setActive(isActiveCheckBox.isSelected());
            pkgToSave.setDescription(descriptionArea.getText().trim());
            pkgToSave.setIncludedItemsDesc(includedItemsArea.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Invalid number format for Price or Capacity.");
            return;
        }

        setFormEditable(false);
        setControlsDisabled(true);
        progressIndicator.setVisible(true);

        final Package finalPkgToSave = pkgToSave;
        Task<Boolean> saveTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return dataManager.savePackage(finalPkgToSave);
            }
        };

        saveTask.setOnSucceeded(e -> Platform.runLater(() -> {
            progressIndicator.setVisible(false);
            setControlsDisabled(false);

            if (saveTask.getValue()) {
                showAlert("Success", "Package " + (isNew ? "added" : "updated") + " successfully!");
                loadPackages();
            } else {
                showAlert("Save Error", "Failed to save the package.");
                setFormEditable(true);
            }
        }));

        saveTask.setOnFailed(e -> Platform.runLater(() -> {
            Throwable error = saveTask.getException();
            LOGGER.log(Level.SEVERE, "Failed to save package", error);
            progressIndicator.setVisible(false);
            setControlsDisabled(false);
            setFormEditable(true);
            showAlert("Application Error", "Could not save package: " + error.getMessage());
        }));

        new Thread(saveTask).start();
    }

    private boolean validatePackageForm() {
        boolean isValid = true;
        clearValidationStyles();

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            nameField.getStyleClass().add("text-field-error");
            isValid = false;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price < 0) {
                priceField.getStyleClass().add("text-field-error");
                isValid = false;
            }
        } catch (NumberFormatException | NullPointerException e) {
            priceField.getStyleClass().add("text-field-error");
            isValid = false;
        }

        try {
            int capacity = Integer.parseInt(capacityField.getText().trim());
            if (capacity <= 0) {
                capacityField.getStyleClass().add("text-field-error");
                isValid = false;
            }
        } catch (NumberFormatException | NullPointerException e) {
            capacityField.getStyleClass().add("text-field-error");
            isValid = false;
        }

        if (!isValid) {
            showAlert("Validation Error", "Please check the highlighted fields. Name is required. Price and Capacity must be valid positive numbers.");
        }
        return isValid;
    }

    private void clearValidationStyles() {
        nameField.getStyleClass().remove("text-field-error");
        priceField.getStyleClass().remove("text-field-error");
        capacityField.getStyleClass().remove("text-field-error");
        descriptionArea.getStyleClass().remove("text-field-error");
        includedItemsArea.getStyleClass().remove("text-field-error");
    }

    @FXML
    void handleDeletePackage(ActionEvent event) {
        Package selectedPkg = packageTableView.getSelectionModel().getSelectedItem();
        if (selectedPkg == null) {
            showAlert("Selection Error", "Please select a package from the table to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion/Deactivation");
        confirmAlert.setHeaderText("Delete/Deactivate Package: " + selectedPkg.getName());
        confirmAlert.setContentText("Are you sure? If the package is used in existing bookings, it will be marked as 'Inactive' instead of being permanently deleted.");
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            setFormEditable(false);
            setControlsDisabled(true);
            progressIndicator.setVisible(true);

            final int packageIdToDelete = selectedPkg.getPackageId();
            final String packageName = selectedPkg.getName();

            Task<Boolean> deleteTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return dataManager.deletePackage(packageIdToDelete);
                }
            };

            deleteTask.setOnSucceeded(e -> Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                setControlsDisabled(false);

                if (deleteTask.getValue()) {
                    Package checkPkg = dataManager.getAllPackages(true)
                                            .stream()
                                            .filter(p -> p.getPackageId() == packageIdToDelete)
                                            .findFirst().orElse(null);
                    if (checkPkg != null && !checkPkg.isActive()) {
                        showAlert("Success", "Package '" + packageName + "' is in use and has been deactivated.");
                    } else {
                        showAlert("Success", "Package '" + packageName + "' deleted successfully.");
                    }
                    loadPackages();
                } else {
                    showAlert("Error", "Could not delete or deactivate the package '" + packageName + "'. It might not exist anymore.");
                    setFormEditable(packageTableView.getSelectionModel().getSelectedItem() != null);
                }
            }));

            deleteTask.setOnFailed(e -> Platform.runLater(() -> {
                Throwable error = deleteTask.getException();
                LOGGER.log(Level.SEVERE, "Failed to delete package", error);
                progressIndicator.setVisible(false);
                setControlsDisabled(false);
                setFormEditable(packageTableView.getSelectionModel().getSelectedItem() != null);
                showAlert("Application Error", "Could not delete package '" + packageName + "': " + error.getMessage());
            }));

            new Thread(deleteTask).start();
        }
    }

    @FXML
    void handleClearPackageForm(ActionEvent event) {
        packageTableView.getSelectionModel().clearSelection();
        clearPackageForm();
        setFormEditable(false);
        deleteButton.setDisable(true);
        currentlySelectedPackage = null;
    }

    private void clearPackageForm() {
        nameField.clear();
        priceField.clear();
        capacityField.clear();
        isActiveCheckBox.setSelected(true);
        descriptionArea.clear();
        includedItemsArea.clear();
        currentlySelectedPackage = null;
        clearValidationStyles();
    }

    private void setFormEditable(boolean editable) {
        nameField.setDisable(!editable);
        priceField.setDisable(!editable);
        capacityField.setDisable(!editable);
        isActiveCheckBox.setDisable(!editable);
        descriptionArea.setDisable(!editable);
        includedItemsArea.setDisable(!editable);
        saveButton.setDisable(!editable);
    }

    private void setControlsDisabled(boolean disabled) {
        packageTableView.setDisable(disabled);
        showInactiveCheckBox.setDisable(disabled);
        newButton.setDisable(disabled);
        clearButton.setDisable(disabled);
        saveButton.setDisable(disabled || nameField.isDisabled());
        deleteButton.setDisable(disabled || packageTableView.getSelectionModel().getSelectedItem() == null);

        if (disabled) {
            setFormEditable(false);
        } else if (!disabled && packageTableView.getSelectionModel().getSelectedItem() != null) {
            setFormEditable(true);
        } else {
            setFormEditable(false);
        }
    }

    private void showAlert(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            showActualAlert(title, message);
        } else {
            Platform.runLater(() -> showActualAlert(title, message));
        }
    }

    private void showActualAlert(String title, String message) {
        Alert.AlertType type = title.toLowerCase().contains("error") ? Alert.AlertType.ERROR :
                              (title.toLowerCase().contains("warning") ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}