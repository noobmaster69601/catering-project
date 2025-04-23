package nagascatering.data; // Or place in nagascatering.db if you prefer

import nagascatering.model.*;
import nagascatering.model.Package;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages application data using in-memory storage.
 * Replaces the database interactions previously handled by DatabaseManager.
 * Note: Data is lost when the application closes.
 */
public class InMemoryDataManager {

    private static final Logger LOGGER = Logger.getLogger(InMemoryDataManager.class.getName());

    // Use thread-safe collections as data might be accessed/modified by background tasks
    private static final Map<Integer, Package> packages = new ConcurrentHashMap<>();
    private static final Map<Integer, Booking> bookings = new ConcurrentHashMap<>();
    private static final Map<Integer, MenuItem> menuItems = new ConcurrentHashMap<>();

    // Use AtomicIntegers for thread-safe ID generation
    private static final AtomicInteger packageIdCounter = new AtomicInteger(1);
    private static final AtomicInteger bookingIdCounter = new AtomicInteger(1);
    private static final AtomicInteger menuItemIdCounter = new AtomicInteger(1);
    private static final AtomicInteger bookingItemIdCounter = new AtomicInteger(1); // For items within bookings

    // Static initializer block to add some sample data
    static {
        LOGGER.info("Initializing In-Memory Data Store with sample data...");
        try {
            // Add some default menu items
            MenuItem item1 = new MenuItem(menuItemIdCounter.getAndIncrement(), "Steamed Rice", 50.0, "per_person", true);
            item1.setDescription("Fluffy white steamed rice.");
            menuItems.put(item1.getItemId(), item1);

            MenuItem item2 = new MenuItem(menuItemIdCounter.getAndIncrement(), "Lechon Belly (Small)", 4500.0, "fixed", true);
            item2.setDescription("Crispy roasted pork belly, good for 15-20 pax.");
            menuItems.put(item2.getItemId(), item2);

            MenuItem item3 = new MenuItem(menuItemIdCounter.getAndIncrement(), "Beef Caldereta", 3000.0, "per_tray", true);
            item3.setDescription("Classic beef stew in tomato sauce, good for 20-25 pax.");
            menuItems.put(item3.getItemId(), item3);

            MenuItem item4 = new MenuItem(menuItemIdCounter.getAndIncrement(), "Iced Tea (Pitcher)", 150.0, "fixed", true);
            item4.setDescription("Standard house blend iced tea.");
            menuItems.put(item4.getItemId(), item4);

            MenuItem item5 = new MenuItem(menuItemIdCounter.getAndIncrement(), "Fruit Salad", 1000.0, "per_tray", false); // Example inactive item
            item5.setDescription("Creamy mixed fruit salad.");
            menuItems.put(item5.getItemId(), item5);

            // Add some default packages
            Package pkg1 = new Package(packageIdCounter.getAndIncrement(), "Basic Birthday Bash", 7500.0, 50, true);
            pkg1.setDescription("A simple package perfect for small birthday celebrations.");
            pkg1.setIncludedItemsDesc("Rice, Beef Caldereta, 1 Pitcher Iced Tea");
            packages.put(pkg1.getPackageId(), pkg1);

            Package pkg2 = new Package(packageIdCounter.getAndIncrement(), "Fiesta Feast", 15000.0, 100, true);
            pkg2.setDescription("A more complete package for larger gatherings.");
            pkg2.setIncludedItemsDesc("Rice, Lechon Belly (Small), Beef Caldereta, 2 Pitchers Iced Tea");
            packages.put(pkg2.getPackageId(), pkg2);

            Package pkg3 = new Package(packageIdCounter.getAndIncrement(), "Grand Celebration (Old)", 25000.0, 150, false); // Example inactive package
            pkg3.setDescription("Previous premium package.");
            pkg3.setIncludedItemsDesc("Extensive menu, contact for details.");
            packages.put(pkg3.getPackageId(), pkg3);

            // Add a sample booking
            Booking booking1 = new Booking();
            booking1.setBookingId(bookingIdCounter.getAndIncrement());
            booking1.setCustomerName("Juan Dela Cruz");
            booking1.setCustomerContact("09171234567");
            booking1.setEventDate(LocalDate.now().plusWeeks(2)); // Upcoming event
            booking1.setEventTime("18:00");
            booking1.setVenueAddress("Sample Function Hall, Naga City");
            booking1.setThemeDescription("Blue and Silver");
            booking1.setNumGuests(45);
            booking1.setSelectedPackageId(pkg1.getPackageId()); // Basic Birthday Bash
            booking1.setBasePackageCost(pkg1.getPrice());
            booking1.setAdditionalItemsCost(0); // No additional items initially
            booking1.setTotalCost(pkg1.getPrice());
            booking1.setBookingStatus("Confirmed");
            booking1.setCustomRequests("Need extra chairs.");
            booking1.setSelectedPackage(pkg1); // Link the object
            booking1.setBookingItems(new ArrayList<>()); // Initialize empty list
            bookings.put(booking1.getBookingId(), booking1);

            LOGGER.info("Sample data loaded.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing sample data", e);
        }
    }

    // --- Package Methods ---
    public List<Package> getAllPackages(boolean includeInactive) {
        LOGGER.log(Level.INFO, "Fetching all packages (Include Inactive: {0})", includeInactive);
        return packages.values().stream()
            .filter(p -> includeInactive || p.isActive())
            .sorted(Comparator.comparing(Package::getName, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    }

    public boolean savePackage(Package pkg) {
        Objects.requireNonNull(pkg, "Package cannot be null");
        boolean isNew = pkg.getPackageId() <= 0;
        if (isNew) {
            pkg.setPackageId(packageIdCounter.getAndIncrement());
            LOGGER.log(Level.INFO, "Inserting new package with ID: {0}", pkg.getPackageId());
        } else {
            LOGGER.log(Level.INFO, "Updating package with ID: {0}", pkg.getPackageId());
        }
        packages.put(pkg.getPackageId(), pkg); // Add or replace
        return true; // Assume success for in-memory
    }

    public boolean deletePackage(int packageId) {
        LOGGER.log(Level.INFO, "Attempting to delete package with ID: {0}", packageId);
        Package pkg = packages.get(packageId);
        if (pkg == null) {
            LOGGER.log(Level.WARNING, "Package not found for deletion: {0}", packageId);
            return false;
        }

        // Check if package is currently used in any booking
        boolean inUse = bookings.values().stream()
            .anyMatch(b -> b.getSelectedPackageId() != null && b.getSelectedPackageId() == packageId);

        if (inUse) {
            // If in use, don't delete, just mark as inactive
            pkg.setActive(false);
            packages.put(packageId, pkg); // Update the map with the inactive package
            LOGGER.log(Level.WARNING, "Package ID {0} is in use. Marked as inactive instead of deleting.", packageId);
            return true; // Indicate success (deactivated)
        } else {
            // If not in use, remove it
            packages.remove(packageId);
            LOGGER.log(Level.INFO, "Package ID {0} deleted successfully.", packageId);
            return true;
        }
    }

    public boolean setPackageActiveStatus(int packageId, boolean isActive) {
        Package pkg = packages.get(packageId);
        if (pkg != null) {
            pkg.setActive(isActive);
            packages.put(packageId, pkg); // Update the map
            LOGGER.log(Level.INFO, "Set active status for package ID {0} to {1}", new Object[]{packageId, isActive});
            return true;
        }
        LOGGER.log(Level.WARNING, "Package not found for status update: {0}", packageId);
        return false;
    }

    // --- Booking Methods ---
    public boolean saveBooking(Booking booking, List<BookingItem> itemsToSave) {
        Objects.requireNonNull(booking, "Booking cannot be null");
        boolean isNew = booking.getBookingId() <= 0;

        // Deep copy the items list to avoid modifying the original list passed in
        List<BookingItem> itemsCopy = (itemsToSave == null) ? new ArrayList<>() :
            itemsToSave.stream().map(item -> {
                BookingItem copiedItem = new BookingItem();
                copiedItem.setBookingItemId(item.getBookingItemId()); // Keep original ID if exists
                copiedItem.setBookingId(booking.getBookingId()); // Will be set/updated below
                copiedItem.setItemId(item.getItemId());
                copiedItem.setQuantity(item.getQuantity());
                copiedItem.setCalculatedItemCost(item.getCalculatedItemCost());
                copiedItem.setMenuItem(item.getMenuItem() != null ? item.getMenuItem() : menuItems.get(item.getItemId()));
                return copiedItem;
            }).collect(Collectors.toList());

        if (isNew) {
            booking.setBookingId(bookingIdCounter.getAndIncrement());
            LOGGER.log(Level.INFO, "Inserting new booking with ID: {0}", booking.getBookingId());
            final int newBookingId = booking.getBookingId(); // Final for lambda
            itemsCopy.forEach(item -> {
                if (item.getBookingItemId() <= 0) {
                    item.setBookingItemId(bookingItemIdCounter.getAndIncrement());
                }
                item.setBookingId(newBookingId); // Link item to the new booking
            });
        } else {
            LOGGER.log(Level.INFO, "Updating booking with ID: {0}", booking.getBookingId());
            final int existingBookingId = booking.getBookingId();
            itemsCopy.forEach(item -> {
                if (item.getBookingItemId() <= 0) {
                    item.setBookingItemId(bookingItemIdCounter.getAndIncrement());
                }
                item.setBookingId(existingBookingId); // Ensure items are linked to this booking
            });
        }

        booking.setBookingItems(itemsCopy);
        if (booking.getSelectedPackageId() != null) {
            booking.setSelectedPackage(packages.get(booking.getSelectedPackageId()));
        } else {
            booking.setSelectedPackage(null);
        }

        bookings.put(booking.getBookingId(), booking); // Add or replace
        return true; // Assume success
    }

    public boolean updateBooking(Booking booking, List<BookingItem> items) {
        LOGGER.log(Level.INFO, "Calling updateBooking (handled by saveBooking) for ID: {0}", booking.getBookingId());
        if (booking == null || booking.getBookingId() <= 0) {
            LOGGER.warning("updateBooking called with invalid booking data.");
            return false;
        }
        return saveBooking(booking, items);
    }

    public Booking getBookingById(int bookingId) {
        LOGGER.log(Level.INFO, "Fetching booking with ID: {0}", bookingId);
        Booking booking = bookings.get(bookingId);
        if (booking != null) {
            if (booking.getSelectedPackageId() != null && booking.getSelectedPackage() == null) {
                booking .setSelectedPackage(packages.get(booking.getSelectedPackageId()));
            }
        } else {
            LOGGER.log(Level.WARNING, "Booking not found for ID: {0}", bookingId);
        }
        return booking;
    }

    public List<Booking> getAllBookings() {
        LOGGER.info("Fetching all bookings");
        List<Booking> allBookingsList = new ArrayList<>();
        bookings.values().forEach(booking -> {
            if (booking.getSelectedPackageId() != null && booking.getSelectedPackage() == null) {
                booking.setSelectedPackage(packages.get(booking.getSelectedPackageId()));
            }
            allBookingsList.add(booking);
        });

        allBookingsList.sort(Comparator.comparing(Booking::getEventDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Booking::getEventTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return allBookingsList;
    }

    public boolean deleteBooking(int bookingId) {
        LOGGER.log(Level.INFO, "Attempting to delete booking with ID: {0}", bookingId);
        Booking removed = bookings.remove(bookingId);
        if (removed != null) {
            LOGGER.log(Level.INFO, "Booking ID {0} deleted successfully.", bookingId);
            return true;
        } else {
            LOGGER.log(Level.WARNING, "Booking not found for deletion: {0}", bookingId);
            return false;
        }
    }

    public List<MenuItem> getAllMenuItems() {
        LOGGER.info("Fetching all active menu items");
        return menuItems.values().stream()
                .filter(MenuItem::isActive)
                .sorted(Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<MenuItem> getAllMenuItems(boolean includeInactive) {
        LOGGER.log(Level.INFO, "Fetching all menu items (Include Inactive: {0})", includeInactive);
        return menuItems.values().stream()
                .filter(m -> includeInactive || m.isActive())
                .sorted(Comparator.comparing(MenuItem::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public MenuItem getMenuItemById(int itemId) {
        LOGGER.log(Level.INFO, "Fetching menu item with ID: {0}", itemId);
        MenuItem item = menuItems.get(itemId);
        if (item == null) {
            LOGGER.log(Level.WARNING, "Menu item not found for ID: {0}", itemId);
        }
        return item;
    }

    public List<BookingItem> getBookingItemsForBooking(int bookingId) {
        LOGGER.log(Level.INFO, "Fetching booking items for booking ID: {0}", bookingId);
        Booking booking = getBookingById(bookingId);
        if (booking != null && booking.getBookingItems() != null) {
            return new ArrayList<>(booking.getBookingItems());
        }
        return new ArrayList<>();
    }
}