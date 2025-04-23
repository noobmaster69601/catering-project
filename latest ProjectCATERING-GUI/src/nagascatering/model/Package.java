package nagascatering.model;

import javafx.beans.property.*;

public class Package {

    private final IntegerProperty packageId = new SimpleIntegerProperty(0); // Default to 0 for new packages
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final IntegerProperty capacity = new SimpleIntegerProperty();
    private final StringProperty includedItemsDesc = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(true); // Default to true

    // --- Constructors ---
    public Package() {}

    // Example constructor
    public Package(int packageId, String name, double price, int capacity, boolean active) {
        setPackageId(packageId);
        setName(name);
        setPrice(price);
        setCapacity(capacity);
        setActive(active);
        // description and includedItemsDesc would need to be set separately or added here
    }


    // --- Property Getters (for JavaFX TableView/Bindings) ---
    public IntegerProperty packageIdProperty() { return packageId; }
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty capacityProperty() { return capacity; }
    public StringProperty includedItemsDescProperty() { return includedItemsDesc; }
    public BooleanProperty activeProperty() { return active; }

    // --- Standard Getters ---
    public int getPackageId() { return packageId.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public double getPrice() { return price.get(); }
    public int getCapacity() { return capacity.get(); }
    public String getIncludedItemsDesc() { return includedItemsDesc.get(); }
    public boolean isActive() { return active.get(); }

    // --- Standard Setters ---
    public void setPackageId(int value) { packageId.set(value); }
    public void setName(String value) { name.set(value); }
    public void setDescription(String value) { description.set(value); }
    public void setPrice(double value) { price.set(value); }
    public void setCapacity(int value) { capacity.set(value); }
    public void setIncludedItemsDesc(String value) { includedItemsDesc.set(value); }
    public void setActive(boolean value) { active.set(value); }

     @Override
    public String toString() {
        // Used by ComboBox to display package names and price
        if (getName() == null || getName().isEmpty()) {
             return "New Package";
        }
        return getName() + " (₱" + String.format("%.2f", getPrice()) + ")";
    }

     // Optional: equals and hashCode for comparing packages if needed
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Package aPackage = (Package) o;
        // If packageId is the unique identifier and > 0, use it
        if (getPackageId() > 0 && aPackage.getPackageId() > 0) {
            return getPackageId() == aPackage.getPackageId();
        }
        // Fallback for new packages or if IDs aren't set (less reliable)
        return java.util.Objects.equals(getName(), aPackage.getName());
    }

    @Override
    public int hashCode() {
        // If using ID for equals, use it for hashCode too
        if (getPackageId() > 0) {
            return java.util.Objects.hash(getPackageId());
        }
        return java.util.Objects.hash(getName());
    }
}