package nagascatering.model;

import java.time.LocalDate;
import java.util.ArrayList; // Added import
import java.util.List; // Added import

public class Booking {

    private int bookingId;
    private String customerName;
    private String customerContact;
    private LocalDate eventDate; // Use java.time types
    private String eventTime; // Store as String HH:mm for simplicity, parse/format as needed
    private String venueAddress;
    private String themeDescription;
    private int numGuests;
    private Integer selectedPackageId; // Use Integer to allow null
    private double basePackageCost;
    private double additionalItemsCost; // Cost from custom items
    private double totalCost;
    private String customRequests;
    private String bookingStatus; // e.g., Pending, Confirmed

    // Optional: Link to the actual Package object
    private Package selectedPackage;
    // Optional: List of custom/additional items - Initialized
    private List<BookingItem> bookingItems = new ArrayList<>();

    // --- Constructors ---
     public Booking() {}

    // --- Getters and Setters (Standard - not JavaFX properties for this example) ---

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerContact() { return customerContact; }
    public void setCustomerContact(String customerContact) { this.customerContact = customerContact; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    public String getThemeDescription() { return themeDescription; }
    public void setThemeDescription(String themeDescription) { this.themeDescription = themeDescription; }

    public int getNumGuests() { return numGuests; }
    public void setNumGuests(int numGuests) { this.numGuests = numGuests; }

    public Integer getSelectedPackageId() { return selectedPackageId; }
    public void setSelectedPackageId(Integer selectedPackageId) { this.selectedPackageId = selectedPackageId; }

    public double getBasePackageCost() { return basePackageCost; }
    public void setBasePackageCost(double basePackageCost) { this.basePackageCost = basePackageCost; }

    public double getAdditionalItemsCost() { return additionalItemsCost; }
    public void setAdditionalItemsCost(double additionalItemsCost) { this.additionalItemsCost = additionalItemsCost; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getCustomRequests() { return customRequests; }
    public void setCustomRequests(String customRequests) { this.customRequests = customRequests; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public Package getSelectedPackage() { return selectedPackage; }
    public void setSelectedPackage(Package selectedPackage) { this.selectedPackage = selectedPackage; }

    // --- Getters/setters/methods for bookingItems list ---
    public List<BookingItem> getBookingItems() {
        return bookingItems;
    }

    public void setBookingItems(List<BookingItem> bookingItems) {
        // Assign a new list if null is passed to avoid NullPointerException
        this.bookingItems = bookingItems == null ? new ArrayList<>() : bookingItems;
    }

    // Optional helper method to add an item
    public void addBookingItem(BookingItem item) {
        if (item != null) {
            this.bookingItems.add(item);
            // Optionally link the item back to this booking's ID if not already set
            // item.setBookingId(this.getBookingId());
        }
    }

    @Override
    public String toString() {
        // Basic representation, customize as needed
        return "Booking ID: " + bookingId + " - " + customerName + " (" + eventDate + ")";
    }
}