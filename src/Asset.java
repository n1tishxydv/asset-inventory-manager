import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Common information shared by hardware and software assets. */
public abstract class Asset implements Comparable<Asset> {
    private final String id;
    private final String name;
    private final LocalDate purchaseDate;
    private final Set<String> tags;

    protected Asset(String id, String name, LocalDate purchaseDate,
                    Collection<String> tags) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        if (purchaseDate == null) {
            throw new IllegalArgumentException("purchaseDate cannot be null");
        }
        this.purchaseDate = purchaseDate;
        this.tags = new HashSet<>();

        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.trim().isEmpty()) {
                    this.tags.add(tag.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    protected static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    /** Returns a read-only view so callers cannot mutate an asset's tags. */
    public final Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public final boolean hasTag(String tag) {
        return tag != null && tags.contains(tag.trim().toLowerCase(Locale.ROOT));
    }

    public abstract String getType();

    /** A type-prefixed key used to prevent duplicate serials/license keys. */
    public abstract String getUniqueIdentifierKey();

    @Override
    public int compareTo(Asset other) {
        int nameComparison = String.CASE_INSENSITIVE_ORDER.compare(name, other.name);
        return nameComparison != 0 ? nameComparison : id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return String.format("%s | %-8s | %s | Purchased: %s | Tags: %s",
                id, getType(), name, purchaseDate, new TreeSet<>(tags));
    }
}
