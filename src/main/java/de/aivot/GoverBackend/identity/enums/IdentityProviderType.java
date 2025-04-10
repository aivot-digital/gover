package de.aivot.GoverBackend.identity.enums;

import de.aivot.GoverBackend.lib.models.Identifiable;

/**
 * Enum representing the different types of identity providers supported in the system.
 *
 * <p>Each identity provider type is associated with a unique integer key, which can be used
 * for identification and mapping purposes. This enum implements the {@link Identifiable} interface
 * to provide a consistent way of retrieving the key for each type.</p>
 *
 * <p>Supported identity provider types:</p>
 * <ul>
 *     <li>{@link #Custom} - A custom identity provider.</li>
 *     <li>{@link #BayernId} - The BayernID identity provider.</li>
 *     <li>{@link #BundId} - The BundID identity provider.</li>
 *     <li>{@link #ShId} - The Servicekonto Schleswig-Holstein identity provider.</li>
 *     <li>{@link #MUK} - The Mein Unternehmenskonto identity provider.</li>
 * </ul>
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Provides a unique key for each identity provider type via the {@link #getKey()} method.</li>
 *     <li>Supports matching of identity provider types using the {@link #matches(Object)} method.</li>
 * </ul>
 *
 * @see Identifiable
 */
public enum IdentityProviderType implements Identifiable<Integer> {
    Custom(0),
    BayernId(1),
    BundId(2),
    ShId(3),
    MUK(4),
    ;

    private final Integer key;

    IdentityProviderType(Integer key) {
        this.key = key;
    }

    @Override
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        switch (other) {
            case IdentityProviderType otherIdentityProviderType -> {
                return otherIdentityProviderType.key.equals(this.key);
            }
            case null, default -> {
                return false;
            }
        }
    }
}
