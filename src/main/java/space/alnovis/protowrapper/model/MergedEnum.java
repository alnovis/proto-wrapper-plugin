package space.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents a merged enum from multiple protocol versions.
 *
 * <p>Contains the unified enum definition with values that work
 * across all versions where this enum is present.</p>
 */
public class MergedEnum {

    private final String name;
    private final List<MergedEnumValue> values;
    private final Set<String> presentInVersions;

    public MergedEnum(String name) {
        this.name = name;
        this.values = new ArrayList<>();
        this.presentInVersions = new LinkedHashSet<>();
    }

    public void addValue(MergedEnumValue value) {
        values.add(value);
    }

    public void addVersion(String version) {
        presentInVersions.add(version);
    }

    public String getName() {
        return name;
    }

    public List<MergedEnumValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    @Override
    public String toString() {
        return String.format("MergedEnum[%s, %d values]", name, values.size());
    }
}
