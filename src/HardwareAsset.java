import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

/** A physical device tracked by its serial number. */
public final class HardwareAsset extends Asset {
    private final String serialNumber;
    private final String manufacturer;

    public HardwareAsset(String id, String name, LocalDate purchaseDate,
                         Collection<String> tags, String serialNumber,
                         String manufacturer) {
        super(id, name, purchaseDate, tags);
        this.serialNumber = requireText(serialNumber, "serialNumber");
        this.manufacturer = requireText(manufacturer, "manufacturer");
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getType() {
        return "Hardware";
    }

    @Override
    public String getUniqueIdentifierKey() {
        return "SERIAL:" + serialNumber.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return super.toString() + " | Serial: " + serialNumber
                + " | Manufacturer: " + manufacturer;
    }
}
