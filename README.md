# Asset Inventory Manager — Java Collections & Generics

## 1. Overview

This is a small, in-memory inventory application for technology hardware and software. It demonstrates inheritance, Java generics, collection selection, safe iteration, sorting, and Java Streams. It uses only the standard Java API and works with Java 8 or later.

## 2. Class design

- **`Asset`** is the abstract base class. It stores an ID, name, purchase date, and tags. Tags are normalized to lowercase and stored in a `HashSet`, which prevents duplicates. Each asset is comparable by name and then ID.
- **`HardwareAsset`** extends `Asset` with a serial number and manufacturer.
- **`SoftwareAsset`** extends `Asset` with a license key and version.
- **`AssetManager<T extends Asset>`** is a type-safe generic manager. It supports add, update, removal, ID lookup, exact-name search, sorting, tag filtering, reports, and a rolling audit log.
- **`AssetManagerDemo`** demonstrates the features through a `main` method.
- **`AssetManagerTest`** is a separate test harness that runs without JUnit or any third-party library.

The manager rejects duplicate IDs and duplicate hardware serial numbers or software license keys. Name searches ignore capitalization and leading/trailing spaces. Updating an asset replaces the record with the same ID and updates the indexes.

## 3. Collection choices

| Collection | Where it is used | Why it fits |
|---|---|---|
| `LinkedHashMap<String, T>` | Main ID-to-asset store | Expected O(1) ID lookup while preserving insertion order for inventory listings. |
| `HashMap<String, HashSet<String>>` | Exact-name index | Expected O(1) access to a name bucket; the set holds IDs of assets with that normalized name. |
| `HashSet<String>` | Unique serial/license identifiers | Expected O(1) average membership checks for duplicate prevention. `Asset` also uses a `HashSet` for unique tags. |
| `ArrayList<T>` | Returned snapshots and sorted results | Fast iteration, compact storage, and efficient sorting with `Collections.sort`. |
| `LinkedList<String>` | Recent audit messages | Efficient append and removal of the oldest entry; limited to the latest 20 messages. |
| `TreeSet<String>` | Combined tag report | Automatically removes duplicate tags and keeps them alphabetically sorted. |
| `TreeMap<String, Long>` | Type-count report | Gives a report whose asset-type keys appear in sorted order. |

The manager is declared `AssetManager<T extends Asset>`, so its collection can only store `Asset` instances or subclasses. The demo uses `AssetManager<Asset>` to keep both hardware and software records in one inventory.

## 4. Required iteration and Streams examples

- **Enhanced for-loop:** `AssetManagerDemo` prints the sorted inventory and audit log using `for (Asset asset : ...)` and `for (String entry : ...)`.
- **Explicit `Iterator`:** `AssetManager.removeByType` removes matching assets through `Iterator.remove()`. This avoids modifying the map through a separate operation while iterating over it.
- **Streams:** `getAssetsWithTag` uses `filter` and `sorted`; `getTypeCountReport` uses `groupingBy` and `counting` for a report.

## 5. Time complexity analysis

Let **n** be the number of assets, **k** be the number of assets with a searched name, **t** be the total number of tags across assets, and **u** be the number of distinct tags. Hash-based complexities below are average-case; occasional resizing can cost more.

| Operation | Expected time | Explanation |
|---|---:|---|
| Add asset | O(1) | Hash-based duplicate checks and index insertions; excludes occasional resize costs. |
| Lookup by ID | O(1) | One `LinkedHashMap` lookup. |
| Remove by ID | O(1) | Map removal plus hash-index updates on average. |
| Update existing asset | O(1) | A constant number of map/set operations on average. |
| Search exact name | O(k log k) | Find the name bucket, retrieve its k assets, then sort the results. |
| List all assets | O(n) | Copies the map values into an `ArrayList`. |
| List sorted by name | O(n log n) | Copies n assets and sorts the list. |
| Filter by tag | O(n log n) | Scans all assets, then sorts matching results; filtering itself is O(n). |
| Build sorted tag report | O(t log u) | Inserts all tags into a `TreeSet`; each insertion costs O(log u). |
| Count by type with Streams | O(n log m) | Groups n assets and uses sorted map keys; m is the number of distinct asset types. |
| Remove by type | O(n) | Inspects each asset once and removes matches using the iterator. |
| Append audit message | O(1) | Adds at the tail; removing the oldest item is O(1) when the cap is exceeded. |

Space usage is O(n + t) for the inventory and its indexes, plus up to 20 audit messages. The name index and identifier set deliberately trade additional memory for faster searches and duplicate checks.

## 6. How to compile and run

Install a JDK 8 or later and open a terminal in the project folder (`asset-inventory-manager`). The source files do not require a build tool.

### Run the demonstration

```bash
mkdir out
javac -d out src/Asset.java src/HardwareAsset.java src/SoftwareAsset.java src/AssetManager.java src/AssetManagerDemo.java
java -cp out AssetManagerDemo
```

### Run the test harness

Compile the test after the application classes have been compiled:

```bash
javac -cp out -d out tests/AssetManagerTest.java
java -cp out AssetManagerTest
```

The test harness throws an `AssertionError` if a check fails. A successful run ends with `ALL TESTS PASSED: 24 checks.` (the individual PASS messages are printed as each check completes).

If the `out` directory already exists, reuse it instead of running `mkdir out` again. The same commands work in a terminal opened from the project directory on Windows, macOS, or Linux.

## 7. Sample output

The demo prints output similar to the following (asset tags are displayed in sorted order):

```text
=== ASSET INVENTORY MANAGER ===
Add laptop: true
Add keyboard: true
Add IDE: true
Add Microsoft 365: true
Duplicate serial rejected: true

Search by ID HW-101: HW-101 | Hardware | Dell XPS 15 | Purchased: 2024-06-12 | Tags: [engineering, laptop] | Serial: SN-1001 | Manufacturer: Dell
Search by name 'intellij idea': [SW-201 | Software | IntelliJ IDEA | Purchased: 2025-01-08 | Tags: [development, ide] | License: LIC-IDE-778 | Version: 2025.1]
...
Type count report (Streams): {Hardware=2, Software=2}
All tags (sorted TreeSet): [development, engineering, ide, laptop, office, peripheral, productivity]

Remove SW-202 by ID: true
Remove remaining software with Iterator: 1 asset(s)
Assets remaining: 2
```

## 8. Notes / limitations

This is an in-memory practice application: data resets when the program exits. IDs are treated as case-sensitive, while serial numbers and license keys are treated as case-insensitive for duplicate prevention. Name search is an exact match rather than a partial-text search.
