import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Generic, in-memory asset repository with indexes for IDs, names, and
 * serial/license identifiers. T can be Asset or a particular asset subtype.
 */
public class AssetManager<T extends Asset> {
    private static final int MAX_AUDIT_ENTRIES = 20;

    // Maintains insertion order while also providing expected O(1) ID lookup.
    private final LinkedHashMap<String, T> assetsById = new LinkedHashMap<>();

    // Normalized name -> IDs; supports efficient exact-name searches.
    private final HashMap<String, HashSet<String>> idsByName = new HashMap<>();

    // Prevents the same hardware serial or software license from being reused.
    private final HashSet<String> uniqueIdentifiers = new HashSet<>();

    // A compact rolling history; additions/removing the oldest entry are O(1).
    private final LinkedList<String> auditLog = new LinkedList<>();

    /** Adds an asset. Returns false if its ID or unique identifier already exists. */
    public boolean add(T asset) {
        Objects.requireNonNull(asset, "asset cannot be null");
        if (assetsById.containsKey(asset.getId())
                || uniqueIdentifiers.contains(asset.getUniqueIdentifierKey())) {
            record("ADD REJECTED: duplicate ID or identifier for " + asset.getId());
            return false;
        }

        assetsById.put(asset.getId(), asset);
        uniqueIdentifiers.add(asset.getUniqueIdentifierKey());
        addToNameIndex(asset);
        record("ADDED: " + asset.getId() + " - " + asset.getName());
        return true;
    }

    /** Looks up an asset by its exact ID. */
    public Optional<T> getById(String id) {
        return Optional.ofNullable(assetsById.get(id));
    }

    /** Removes one asset by ID. Returns false when no asset has that ID. */
    public boolean removeById(String id) {
        T removed = assetsById.remove(id);
        if (removed == null) {
            return false;
        }
        removeFromNameIndex(removed);
        uniqueIdentifiers.remove(removed.getUniqueIdentifierKey());
        record("REMOVED: " + removed.getId() + " - " + removed.getName());
        return true;
    }

    /**
     * Replaces the asset with the same ID. The ID is fixed by the supplied
     * object; a serial/license key cannot collide with another stored asset.
     */
    public boolean update(T updatedAsset) {
        Objects.requireNonNull(updatedAsset, "updatedAsset cannot be null");
        T current = assetsById.get(updatedAsset.getId());
        if (current == null) {
            return false;
        }

        String oldIdentifier = current.getUniqueIdentifierKey();
        String newIdentifier = updatedAsset.getUniqueIdentifierKey();
        if (!oldIdentifier.equals(newIdentifier)
                && uniqueIdentifiers.contains(newIdentifier)) {
            record("UPDATE REJECTED: duplicate identifier for " + updatedAsset.getId());
            return false;
        }

        removeFromNameIndex(current);
        uniqueIdentifiers.remove(oldIdentifier);
        assetsById.put(updatedAsset.getId(), updatedAsset);
        uniqueIdentifiers.add(newIdentifier);
        addToNameIndex(updatedAsset);
        record("UPDATED: " + updatedAsset.getId() + " - " + updatedAsset.getName());
        return true;
    }

    /** Searches for an exact name, ignoring case and surrounding whitespace. */
    public List<T> searchByName(String name) {
        List<T> results = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            return results;
        }

        Set<String> ids = idsByName.get(normalizeName(name));
        if (ids != null) {
            for (String id : ids) {
                T asset = assetsById.get(id);
                if (asset != null) {
                    results.add(asset);
                }
            }
        }
        Collections.sort(results);
        return results;
    }

    /** Returns a snapshot in the order the assets were added. */
    public List<T> getAll() {
        return new ArrayList<>(assetsById.values());
    }

    /** Returns a new list sorted by asset name, then by ID. */
    public List<T> getSortedByName() {
        List<T> results = new ArrayList<>(assetsById.values());
        Collections.sort(results);
        return results;
    }

    /** Uses a stream filter to find assets carrying a tag (case-insensitive). */
    public List<T> getAssetsWithTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return assetsById.values().stream()
                .filter(asset -> asset.hasTag(tag))
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Builds a sorted, unique list of all tags across the inventory. */
    public Set<String> getAllTagsSorted() {
        TreeSet<String> tags = new TreeSet<>();
        for (T asset : assetsById.values()) {
            tags.addAll(asset.getTags());
        }
        return Collections.unmodifiableSet(tags);
    }

    /** Uses a stream grouping operation to report the count for each asset type. */
    public Map<String, Long> getTypeCountReport() {
        return assetsById.values().stream().collect(
                Collectors.groupingBy(Asset::getType, TreeMap::new, Collectors.counting()));
    }

    /**
     * Removes every asset of the requested subtype using an explicit Iterator.
     * Iterator.remove() avoids ConcurrentModificationException.
     */
    public int removeByType(Class<? extends Asset> assetClass) {
        Objects.requireNonNull(assetClass, "assetClass cannot be null");
        int removedCount = 0;
        Iterator<Map.Entry<String, T>> iterator = assetsById.entrySet().iterator();
        while (iterator.hasNext()) {
            T asset = iterator.next().getValue();
            if (assetClass.isInstance(asset)) {
                iterator.remove();
                removeFromNameIndex(asset);
                uniqueIdentifiers.remove(asset.getUniqueIdentifierKey());
                record("REMOVED BY TYPE: " + asset.getId() + " - " + asset.getName());
                removedCount++;
            }
        }
        return removedCount;
    }

    /** Returns a read-only copy of the most recent audit messages. */
    public List<String> getAuditLog() {
        return Collections.unmodifiableList(new ArrayList<>(auditLog));
    }

    public int size() {
        return assetsById.size();
    }

    public boolean isEmpty() {
        return assetsById.isEmpty();
    }

    private void addToNameIndex(T asset) {
        idsByName.computeIfAbsent(normalizeName(asset.getName()), key -> new HashSet<>())
                .add(asset.getId());
    }

    private void removeFromNameIndex(T asset) {
        String key = normalizeName(asset.getName());
        HashSet<String> ids = idsByName.get(key);
        if (ids != null) {
            ids.remove(asset.getId());
            if (ids.isEmpty()) {
                idsByName.remove(key);
            }
        }
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private void record(String message) {
        auditLog.addLast(message);
        if (auditLog.size() > MAX_AUDIT_ENTRIES) {
            auditLog.removeFirst();
        }
    }
}
