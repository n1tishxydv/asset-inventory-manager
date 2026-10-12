import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

/** A software product/license tracked by its license key. */
public final class SoftwareAsset extends Asset {
    private final String licenseKey;
    private final String version;

    public SoftwareAsset(String id, String name, LocalDate purchaseDate,
                         Collection<String> tags, String licenseKey,
                         String version) {
        super(id, name, purchaseDate, tags);
        this.licenseKey = requireText(licenseKey, "licenseKey");
        this.version = requireText(version, "version");
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String getType() {
        return "Software";
    }

    @Override
    public String getUniqueIdentifierKey() {
        return "LICENSE:" + licenseKey.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return super.toString() + " | License: " + licenseKey
                + " | Version: " + version;
    }
}
