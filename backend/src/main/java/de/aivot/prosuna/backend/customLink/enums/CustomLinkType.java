package de.aivot.prosuna.backend.customLink.enums;

/**
 * Usage contexts supported by configurable links.
 *
 * <p>Database values are part of the persisted contract. Existing values must not be changed or reused; new link
 * types need a new value.</p>
 */
public enum CustomLinkType {
    Dashboard(0),
    ;

    private final short databaseValue;

    CustomLinkType(int databaseValue) {
        if (databaseValue < Short.MIN_VALUE || databaseValue > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Custom link type database value must fit into a smallint");
        }
        this.databaseValue = (short) databaseValue;
    }

    public short getDatabaseValue() {
        return databaseValue;
    }
}
