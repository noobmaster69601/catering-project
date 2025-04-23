package nagascatering.model;

import javafx.beans.property.*;
import java.util.Objects; // Import Objects for equals/hashCode

// Represents an item from the menu_items table
public class MenuItem {

    private final IntegerProperty itemId = new SimpleIntegerProperty(0); // Default 0 for new
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final DoubleProperty costPerUnit = new SimpleDoubleProperty();
    private final StringProperty unitType = new SimpleStringProperty(); // e.g., per_person, per_tray, fixed
    private final BooleanProperty active = new SimpleBooleanProperty(true);

    // --- Constructors ---
    public MenuItem() {}

    // Add constructor if needed
    public MenuItem(int itemId, String name, double costPerUnit, String unitType, boolean active) {
         setItemId(itemId);
         setName(name);
         setCostPerUnit(costPerUnit);
         setUnitType(unitType);
         setActive(active);
         // description set separately
    }


    // --- Property Getters ---
    public IntegerProperty itemIdProperty() { return itemId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }
    public DoubleProperty costPerUnitProperty() { return costPerUnit; }
    public StringProperty unitTypeProperty() { return unitType; }
    public BooleanProperty activeProperty() { return active; }

    // --- Standard Getters ---
    public int getItemId() { return itemId.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public double getCostPerUnit() { return costPerUnit.get(); }
    public String getUnitType() { return unitType.get(); }
    public boolean isActive() { return active.get(); }

    // --- Standard Setters ---
    public void setItemId(int value) { itemId.set(value); }
    public void setName(String value) { name.set(value); }
    public void setDescription(String value) { description.set(value); }
    public void setCostPerUnit(double value) { costPerUnit.set(value); }
    public void setUnitType(String value) { unitType.set(value); }
    public void setActive(boolean value) { active.set(value); }

    @Override
    public String toString() {
        // Used by ComboBoxes or lists for display
        String nameStr = name.get() != null ? name.get() : "Unnamed Item";
        String costStr = String.format("%.2f", costPerUnit.get());
        String unitStr = unitType.get() != null ? unitType.get() : "unit";
        // Example: "Lechon Belly (5000.00/tray)" or "Rice (50.00/per_person)"
        return String.format("%s (₱%s/%s)", nameStr, costStr, unitStr);
    }

     // Optional: equals and hashCode for comparing menu items, especially in lists/sets
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        // Use itemId if it's assigned (> 0), otherwise rely on name (less reliable for unsaved items)
        if (getItemId() > 0 && menuItem.getItemId() > 0) {
            return getItemId() == menuItem.getItemId();
        }
        return Objects.equals(getName(), menuItem.getName());
    }

    @Override
    public int hashCode() {
         if (getItemId() > 0) {
            return Objects.hash(getItemId());
        }
        return Objects.hash(getName());
    }
}