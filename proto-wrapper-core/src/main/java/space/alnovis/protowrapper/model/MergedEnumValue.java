package space.alnovis.protowrapper.model;

import java.util.*;

/**
 * Represents a merged enum value from multiple protocol versions.
 */
public class MergedEnumValue {

    private final String name;
    private final String javaName;
    private final int number;
    private final Set<String> presentInVersions;

    public MergedEnumValue(EnumInfo.EnumValue value, String version) {
        this.name = value.getName();
        this.javaName = value.getJavaName();
        this.number = value.getNumber();
        this.presentInVersions = new LinkedHashSet<>();
        this.presentInVersions.add(version);
    }

    public void addVersion(String version) {
        presentInVersions.add(version);
    }

    public String getName() {
        return name;
    }

    public String getJavaName() {
        return javaName;
    }

    public int getNumber() {
        return number;
    }

    public Set<String> getPresentInVersions() {
        return Collections.unmodifiableSet(presentInVersions);
    }

    @Override
    public String toString() {
        return String.format("MergedEnumValue[%s=%d, versions=%s]", name, number, presentInVersions);
    }
}
