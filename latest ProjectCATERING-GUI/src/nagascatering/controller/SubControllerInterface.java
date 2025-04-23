package nagascatering.controller;

/**
 * Interface for sub-controllers to allow the MainController
 * to pass a reference to itself for navigation and potentially other interactions.
 */
public interface SubControllerInterface {
    /**
     * Sets the reference to the main application controller.
     * This method is typically called by the MainController immediately after
     * loading the sub-view's FXML.
     *
     * @param mainController The instance of the MainController. Must not be null.
     */
    void setMainController(MainController mainController);

    /**
     * Optional method to pass data to the controller when it's navigated to.
     * Controllers implementing this should safely cast the data object and handle
     * null or unexpected data types gracefully.
     * <p>
     * Example Implementation:
     * <pre>{@code
     * @Override
     * public void initializeData(Object data) {
     * if (data instanceof Booking) {
     * this.bookingToEdit = (Booking) data;
     * populateFormWithBooking(this.bookingToEdit);
     * } else if (data != null) {
     * LOGGER.warning("Received unexpected data type: " + data.getClass().getName());
     * }
     * // If no data is passed (data == null), initialize for a new entry.
     * }
     * }</pre>
     *
     * @param data The data object passed during navigation (can be null).
     */
     default void initializeData(Object data) {
         // Default implementation does nothing.
         // Controllers needing data should override this method.
     }
}