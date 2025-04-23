package nagascatering.model;

import javafx.beans.property.*;
import java.util.Objects; // For equals/hashCode

// Represents a link between a booking and a menu item from booking_items table
public class BookingItem {

    private final IntegerProperty bookingItemId = new SimpleIntegerProperty(0); // 0 for new
    private final IntegerProperty bookingId = new SimpleIntegerProperty(); // FK to bookings
    private final IntegerProperty itemId = new SimpleIntegerProperty();    // FK to menu_items
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final DoubleProperty calculatedItemCost = new SimpleDoubleProperty(); // Total cost for this line item (qty * unit_cost)

    // Hold the associated MenuItem object for easy access to details like name, unit cost etc.
    private MenuItem menuItem;

    // --- Constructors ---
    public BookingItem() {}

    // --- Property Getters (useful for TableView columns if needed) ---
    public IntegerProperty bookingItemIdProperty() { return bookingItemId; }
    public IntegerProperty bookingIdProperty() { return bookingId; }
    public IntegerProperty itemIdProperty() { return itemId; }
    public IntegerProperty quantityProperty() { return quantity; }
    public DoubleProperty calculatedItemCostProperty() { return calculatedItemCost; }
    // Property for MenuItem name (derived) - Useful for TableView display
     public StringProperty itemNameProperty() {
        // Creates a read-only property bound to the MenuItem's name
        // Requires MenuItem nameProperty to be available
        if (menuItem != null) {
            return menuItem.nameProperty(); // Directly return if MenuItem uses JavaFX properties
        } else {
            // Fallback if menuItem is null or doesn't use properties
             return new SimpleStringProperty("Item ID: " + getItemId());
        }
        // Alternative if MenuItem doesn't use properties:
        // String name = (menuItem != null) ? menuItem.getName() : "Item ID: " + getItemId();
        // return new SimpleStringProperty(name);
    }


    // --- Standard Getters ---
    public int getBookingItemId() { return bookingItemId.get(); }
    public int getBookingId() { return bookingId.get(); }
    public int getItemId() { return itemId.get(); }
    public int getQuantity() { return quantity.get(); }
    public double getCalculatedItemCost() { return calculatedItemCost.get(); }
    public MenuItem getMenuItem() { return menuItem; }

    // --- Standard Setters ---
    public void setBookingItemId(int value) { bookingItemId.set(value); }
    public void setBookingId(int value) { bookingId.set(value); }
    public void setItemId(int value) { itemId.set(value); }
    public void setQuantity(int value) { quantity.set(value); }
    public void setCalculatedItemCost(double value) { calculatedItemCost.set(value); }
    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
        // If MenuItem changes, ensure itemId property is also updated if needed
        if (menuItem != null) {
            this.itemId.set(menuItem.getItemId());
        }
    }

     @Override
    public String toString() {
         // Provides a readable representation for ListView or debugging
         String itemName = (menuItem != null && menuItem.getName() != null) ? menuItem.getName() : "Item ID: " + getItemId();
         return String.format("%s x %d = ₱%.2f", itemName, getQuantity(), getCalculatedItemCost());
    }

    // Optional: equals/hashCode if needed to compare BookingItems
    // Typically based on bookingItemId if persisted, or itemId+bookingId if transient
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingItem that = (BookingItem) o;
        // If IDs are set and positive, use them for comparison
        if (getBookingItemId() > 0 && that.getBookingItemId() > 0) {
            return getBookingItemId() == that.getBookingItemId();
        }
        // Fallback for unsaved items (compare by item ID and booking ID if available)
        if (getBookingId() > 0 && that.getBookingId() > 0) {
             return getBookingId() == that.getBookingId() && getItemId() == that.getItemId();
        }
        // Less reliable fallback if IDs are not set
        return getItemId() == that.getItemId() && Objects.equals(getMenuItem(), that.getMenuItem());
    }

    @Override
    public int hashCode() {
        if (getBookingItemId() > 0) {
            return Objects.hash(getBookingItemId());
        }
         if (getBookingId() > 0) {
             return Objects.hash(getBookingId(), getItemId());
         }
        return Objects.hash(getItemId(), getMenuItem());
    }
}