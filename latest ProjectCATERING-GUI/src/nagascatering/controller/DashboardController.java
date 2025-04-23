package nagascatering.controller;

import nagascatering.data.InMemoryDataManager; // Added
import nagascatering.model.Booking;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator; // Added

import java.net.URL;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DashboardController implements Initializable, SubControllerInterface {

    @FXML private Label upcomingEventsLabel;
    @FXML private Label statsLabel;
    @FXML private ProgressIndicator progressIndicator; // Added fx:id="progressIndicator" in FXML

    private MainController mainController;
    private InMemoryDataManager dataManager; // Added

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        dataManager = new InMemoryDataManager(); // Added
        if (progressIndicator != null) progressIndicator.setVisible(false); // Hide initially
        loadDashboardData();
    }

    private void loadDashboardData() {
        if (progressIndicator != null) progressIndicator.setVisible(true);
        upcomingEventsLabel.setText("Upcoming Events: (Loading...)");
        if(statsLabel != null) statsLabel.setText("Statistics: (Loading...)");

        Task<List<Booking>> loadUpcomingTask = createUpcomingEventsTask();
        Task<String> loadStatsTask = createStatisticsTask(); // Assuming statsLabel exists

        loadUpcomingTask.setOnSucceeded(e -> Platform.runLater(() -> {
            handleUpcomingEventsSuccess(loadUpcomingTask.getValue());
            checkLoadCompletion(loadStatsTask); // Check if stats task is done
        }));
        loadUpcomingTask.setOnFailed(e -> Platform.runLater(() -> {
            handleUpcomingEventsFailure(loadUpcomingTask.getException());
            checkLoadCompletion(loadStatsTask); // Check if stats task is done
        }));

        if (loadStatsTask != null) {
            loadStatsTask.setOnSucceeded(e -> Platform.runLater(() -> {
                handleStatisticsSuccess(loadStatsTask.getValue());
                checkLoadCompletion(loadUpcomingTask); // Check if upcoming task is done
            }));
            loadStatsTask.setOnFailed(e -> Platform.runLater(() -> {
                handleStatisticsFailure(loadStatsTask.getException());
                checkLoadCompletion(loadUpcomingTask); // Check if upcoming task is done
            }));

            new Thread(loadStatsTask).start(); // Start stats task
        } else {
            checkLoadCompletion(loadUpcomingTask);
        }

        new Thread(loadUpcomingTask).start(); // Start upcoming events task
    }

    private Task<List<Booking>> createUpcomingEventsTask() {
         return new Task<List<Booking>>() {
             @Override
             protected List<Booking> call() throws Exception {
                 List<Booking> allBookings = dataManager.getAllBookings(); // Updated
                 LocalDate today = LocalDate.now();
                 return allBookings.stream()
                         .filter(b -> b.getEventDate() != null && !b.getEventDate().isBefore(today))
                         .sorted(Comparator.comparing(Booking::getEventDate)
                                   .thenComparing(Booking::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())))
                         .limit(5)
                         .collect(Collectors.toList());
            }
         };
    }

     private Task<String> createStatisticsTask() {
         if (statsLabel == null) return null; // Don't create task if label doesn't exist

         return new Task<String>() {
             @Override
             protected String call() throws Exception {
                 List<Booking> allBookings = dataManager.getAllBookings(); // Updated
                 long totalBookings = allBookings.size();
                 long confirmedBookings = allBookings.stream()
                                                    .filter(b -> "Confirmed".equalsIgnoreCase(b.getBookingStatus()))
                                                    .count();
                 long pendingBookings = allBookings.stream()
                                                    .filter(b -> "Pending".equalsIgnoreCase(b.getBookingStatus()))
                                                    .count();
                 return String.format("Total Bookings: %d\nConfirmed: %d | Pending: %d",
                                      totalBookings, confirmedBookings, pendingBookings);
             }
         };
     }

     private void handleUpcomingEventsSuccess(List<Booking> upcoming) {
         if (upcoming.isEmpty()) {
             upcomingEventsLabel.setText("No upcoming events found.");
         } else {
             StringBuilder summary = new StringBuilder("Next " + upcoming.size() + " Upcoming Events:\n");
             for (Booking b : upcoming) {
                 summary.append(" • ")
                        .append(b.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                 if (b.getEventTime() != null && !b.getEventTime().isEmpty()){
                       summary.append(" at ").append(b.getEventTime());
                 }
                 summary.append(": ")
                        .append(b.getCustomerName() != null ? b.getCustomerName() : "N/A");
                 if (b.getSelectedPackage() != null && b.getSelectedPackage().getName() != null) {
                       summary.append(" [").append(b.getSelectedPackage().getName()).append("]");
                  } else if (b.getSelectedPackageId() != null) {
                       summary.append(" [Package ID: ").append(b.getSelectedPackageId()).append("]");
                  }
                 summary.append("\n");
             }
             upcomingEventsLabel.setText(summary.toString());
         }
     }

     private void handleUpcomingEventsFailure(Throwable error) {
          upcomingEventsLabel.setText("Error loading upcoming events.");
          LOGGER.log(Level.SEVERE, "Failed to load upcoming events for dashboard", error);
     }

      private void handleStatisticsSuccess(String stats) {
         if (statsLabel != null) {
             statsLabel.setText("Statistics:\n" + stats);
         }
     }

      private void handleStatisticsFailure(Throwable error) {
         if (statsLabel != null) {
             statsLabel.setText("Error loading statistics.");
         }
          LOGGER.log(Level.SEVERE, "Failed to load statistics for dashboard", error);
     }

       private void checkLoadCompletion(Task<?> otherTask) {
          if (otherTask.isDone()) {
               if (progressIndicator != null) progressIndicator.setVisible(false);
          }
     }

    @FXML
    void handleManagePackages(ActionEvent event) {
        navigateTo("/nagascatering/view/PackageManager.fxml");
    }

    @FXML
    void handleNewBooking(ActionEvent event) {
         navigateTo("/nagascatering/view/BookingForm.fxml");
    }

    @FXML
    void handleViewBookings(ActionEvent event) {
         navigateTo("/nagascatering/view/BookingListView.fxml");
    }

    @FXML
    void handleRefreshDashboard(ActionEvent event) {
        loadDashboardData();
    }

    private void navigateTo(String fxmlPath) {
         if (mainController != null) {
             mainController.navigateTo(fxmlPath);
         } else {
             LOGGER.severe("MainController reference not set in DashboardController. Cannot navigate.");
             showAlert("Navigation Error", "Cannot navigate. Main controller reference is missing.");
         }
    }

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
         alert.setHeaderText(null);
         alert.setContentText(message);
         alert.showAndWait();
     }
}