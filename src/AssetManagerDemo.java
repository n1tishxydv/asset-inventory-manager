import java.time.LocalDate;
import java.util.Arrays;

/** Command-line demonstration of the Asset Inventory Manager features. */
public class AssetManagerDemo {
    public static void main(String[] args) {
        AssetManager<Asset> manager = new AssetManager<>();

        HardwareAsset laptop = new HardwareAsset(
                "HW-101", "Dell XPS 15", LocalDate.of(2024, 6, 12),
                Arrays.asList("Laptop", "Engineering"), "SN-1001", "Dell");
        HardwareAsset keyboard = new HardwareAsset(
                "HW-102", "Logitech MX Keys", LocalDate.of(2025, 2, 3),
                Arrays.asList("Peripheral", "Office"), "SN-1002", "Logitech");
        SoftwareAsset ide = new SoftwareAsset(
                "SW-201", "IntelliJ IDEA", LocalDate.of(2025, 1, 8),
                Arrays.asList("Development", "IDE"), "LIC-IDE-778", "2025.1");
        SoftwareAsset office = new SoftwareAsset(
                "SW-202", "Microsoft 365", LocalDate.of(2024, 11, 15),
                Arrays.asList("Office", "Productivity"), "LIC-M365-448", "2024");

        System.out.println("=== ASSET INVENTORY MANAGER ===");
        System.out.println("Add laptop: " + manager.add(laptop));
        System.out.println("Add keyboard: " + manager.add(keyboard));
        System.out.println("Add IDE: " + manager.add(ide));
        System.out.println("Add Microsoft 365: " + manager.add(office));

        HardwareAsset duplicateSerial = new HardwareAsset(
                "HW-103", "Spare Laptop", LocalDate.of(2025, 4, 10),
                Arrays.asList("Laptop"), "SN-1001", "Dell");
        System.out.println("Duplicate serial rejected: " + !manager.add(duplicateSerial));

        System.out.println("\nSearch by ID HW-101: " + manager.getById("HW-101").orElse(null));
        System.out.println("Search by name 'intellij idea': " + manager.searchByName("intellij idea"));

        HardwareAsset renamedKeyboard = new HardwareAsset(
                "HW-102", "Logitech MX Keys Mini", LocalDate.of(2025, 2, 3),
                Arrays.asList("Peripheral", "Office"), "SN-1002", "Logitech");
        System.out.println("Update keyboard: " + manager.update(renamedKeyboard));

        System.out.println("\nAssets sorted by name (enhanced for-loop):");
        for (Asset asset : manager.getSortedByName()) {
            System.out.println("  " + asset);
        }

        System.out.println("\nAssets tagged 'development' (Stream filter): "
                + manager.getAssetsWithTag("development"));
        System.out.println("Type count report (Streams): " + manager.getTypeCountReport());
        System.out.println("All tags (sorted TreeSet): " + manager.getAllTagsSorted());

        System.out.println("\nRemove SW-202 by ID: " + manager.removeById("SW-202"));
        System.out.println("Remove remaining software with Iterator: "
                + manager.removeByType(SoftwareAsset.class) + " asset(s)");
        System.out.println("Assets remaining: " + manager.size());

        System.out.println("\nRecent audit log:");
        for (String entry : manager.getAuditLog()) {
            System.out.println("  " + entry);
        }
    }
}
