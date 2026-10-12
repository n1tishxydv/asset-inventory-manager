import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Small dependency-free test harness. It throws an error if a check fails. */
public class AssetManagerTest {
    private static int passed = 0;

    public static void main(String[] args) {
        AssetManager<Asset> manager = new AssetManager<>();
        HardwareAsset laptop = new HardwareAsset(
                "HW-1", "Dell Laptop", LocalDate.of(2024, 1, 10),
                Arrays.asList("Engineering", "Laptop"), "SER-1", "Dell");
        HardwareAsset monitor = new HardwareAsset(
                "HW-2", "LG Monitor", LocalDate.of(2024, 3, 20),
                Arrays.asList("Office"), "SER-2", "LG");
        SoftwareAsset ide = new SoftwareAsset(
                "SW-1", "IntelliJ IDEA", LocalDate.of(2025, 2, 1),
                Arrays.asList("Development"), "LIC-1", "2025.1");

        check(manager.isEmpty(), "new manager starts empty");
        check(manager.add(laptop), "adds first asset");
        check(manager.add(monitor), "adds second asset");
        check(manager.add(ide), "adds software asset");
        check(!manager.add(new HardwareAsset("HW-3", "Duplicate serial",
                LocalDate.of(2025, 1, 1), Arrays.asList("Laptop"), "SER-1", "Dell")),
                "rejects duplicate serial number");
        check(!manager.add(new HardwareAsset("HW-1", "Duplicate ID",
                LocalDate.of(2025, 1, 1), Arrays.asList("Laptop"), "SER-9", "Dell")),
                "rejects duplicate ID");
        check(manager.size() == 3, "duplicate attempts do not change inventory size");
        check(manager.getById("HW-1").isPresent(), "searches by ID");
        check(!manager.getById("MISSING").isPresent(), "missing ID returns empty Optional");
        check(manager.searchByName("  dell laptop ").size() == 1,
                "searches by name ignoring case and whitespace");
        check(manager.getAssetsWithTag("ENGINEERING").size() == 1,
                "filters by tag ignoring case");

        HardwareAsset renamedLaptop = new HardwareAsset(
                "HW-1", "Dell Laptop Gen 2", LocalDate.of(2024, 1, 10),
                Arrays.asList("Engineering", "Laptop"), "SER-1", "Dell");
        check(manager.update(renamedLaptop), "updates existing asset");
        check(manager.searchByName("Dell Laptop").isEmpty(), "update removes old name index");
        check(manager.searchByName("Dell Laptop Gen 2").size() == 1,
                "update adds new name index");
        check(!manager.update(new HardwareAsset("UNKNOWN", "Missing",
                LocalDate.of(2024, 1, 1), Arrays.asList("test"), "SER-UNKNOWN", "Generic")),
                "does not update a missing asset");

        List<Asset> sorted = manager.getSortedByName();
        check(sorted.get(0).getName().equals("Dell Laptop Gen 2"), "returns name-sorted list");
        check(manager.getTypeCountReport().get("Hardware") == 2L,
                "stream report counts hardware assets");
        check(manager.getTypeCountReport().get("Software") == 1L,
                "stream report counts software assets");
        check(manager.getAllTagsSorted().contains("engineering"), "collects normalized tags");
        check(manager.removeById("SW-1"), "removes by ID");
        check(!manager.removeById("SW-1"), "cannot remove the same asset twice");

        manager.add(new SoftwareAsset("SW-2", "Text Editor", LocalDate.of(2025, 4, 2),
                Arrays.asList("Development"), "LIC-2", "1.0"));
        check(manager.removeByType(SoftwareAsset.class) == 1,
                "iterator removes assets of a selected type");
        check(manager.size() == 2, "inventory size is correct after removals");
        check(!manager.getAuditLog().isEmpty(), "audit log records operations");

        System.out.println("ALL TESTS PASSED: " + passed + " checks.");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FAILED: " + description);
        }
        passed++;
        System.out.println("PASS: " + description);
    }
}
