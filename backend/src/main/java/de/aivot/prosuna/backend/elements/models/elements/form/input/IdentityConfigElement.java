package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.utils.SpringContext;
import de.aivot.prosuna.backend.utils.StringUtils;
import tools.jackson.core.JacksonException;

import java.util.*;

public class IdentityConfigElement extends BaseInputElement<List<IdentityConfigElementSlot>> {
    private static final Set<IdentityProviderType> IDENTITY_PROVIDER_TYPES_REQUIRING_TRUST_LEVEL = Set.of(
            IdentityProviderType.BayernId,
            IdentityProviderType.BundId,
            IdentityProviderType.ShId
    );

    public IdentityConfigElement() {
        super(ElementType.IdentityConfig);
    }

    @Override
    public List<IdentityConfigElementSlot> formatValue(Object value) {
        if (value == null) {
            return null;
        }

        var om = JsonMapperFactory
                .getInstance();

        return switch (value) {
            case List<?> valueList -> {
                List<IdentityConfigElementSlot> list = new ArrayList<>();
                for (Object item : valueList) {
                    list.add(om.convertValue(item, IdentityConfigElementSlot.class));
                }
                yield list;
            }
            case Object[] valueArray -> {
                List<IdentityConfigElementSlot> list = new ArrayList<>();
                for (Object item : valueArray) {
                    list.add(om.convertValue(item, IdentityConfigElementSlot.class));
                }
                yield list;
            }
            case String valueString -> {
                try {
                    yield JsonMapperFactory
                            .getInstance()
                            .readerForListOf(IdentityConfigElementSlot.class)
                            .readValue(valueString);
                } catch (JacksonException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> null;
        };
    }

    @Override
    public void performValidation(List<IdentityConfigElementSlot> value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
            return;
        }

        var identityProviderCache = new HashMap<UUID, Optional<IdentityProviderEntity>>();
        var validationErrors = new ArrayList<String>();
        var missingTrustLevelProviderNames = new ArrayList<String>();

        for (var slot : value) {
            if (!hasAcquisitionMethod(slot)) {
                validationErrors.add("Für jede Identität muss mindestens ein Nutzerkontenanbieter oder die direkte E-Mail-Eingabe aktiviert werden.");
                continue;
            }

            validateSelectedOptions(slot, identityProviderCache, validationErrors, missingTrustLevelProviderNames);
        }

        if (!missingTrustLevelProviderNames.isEmpty()) {
            validationErrors.add(createMissingTrustLevelError(missingTrustLevelProviderNames));
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(
                    this,
                    String.join(" ", validationErrors.stream().distinct().toList())
            );
        }
    }

    private void validateSelectedOptions(
            IdentityConfigElementSlot slot,
            Map<UUID, Optional<IdentityProviderEntity>> identityProviderCache,
            List<String> validationErrors,
            List<String> missingTrustLevelProviderNames
    ) throws ValidationException {
        if (slot == null || slot.getOptions() == null) {
            return;
        }

        for (var option : slot.getOptions()) {
            if (option == null || option.getIdentityProviderKey() == null) {
                continue;
            }

            var identityProvider = getIdentityProvider(option.getIdentityProviderKey(), identityProviderCache);
            if (identityProvider.isEmpty()) {
                validationErrors.add("Ein ausgewählter Identitätsanbieter konnte nicht gefunden werden.");
                continue;
            }

            try {
                var hasCommunicationBinding = !SpringContext
                        .getBean(CommunicationService.class)
                        .getUsableBindings(identityProvider.get())
                        .isEmpty();
                if (!hasCommunicationBinding) {
                    validationErrors.add(String.format(
                            "Für den Identitätsanbieter \"%s\" ist keine verwendbare Kommunikationsanbindung konfiguriert.",
                            identityProvider.get().getName()
                    ));
                }
            } catch (Exception e) {
                throw new ValidationException(this, "Die Kommunikationsanbindungen konnten nicht überprüft werden.");
            }

            if (requiresTrustLevel(identityProvider.get()) && !hasSelectedTrustLevel(option)) {
                missingTrustLevelProviderNames.add(identityProvider.get().getName());
            }
        }
    }

    private String createMissingTrustLevelError(List<String> identityProviderNames) {
        var distinctProviderNames = identityProviderNames
                .stream()
                .filter(StringUtils::isNotNullOrEmpty)
                .distinct()
                .toList();

        if (distinctProviderNames.size() == 1) {
            return String.format(
                    "Für den Identitätsanbieter \"%s\" muss ein Mindest-Vertrauensniveau ausgewählt werden.",
                    distinctProviderNames.get(0)
            );
        }

        return String.format(
                "Für die Identitätsanbieter %s muss ein Mindest-Vertrauensniveau ausgewählt werden.",
                String.join(
                        ", ",
                        distinctProviderNames
                                .stream()
                                .map(name -> String.format("\"%s\"", name))
                                .toList()
                )
        );
    }

    private Optional<IdentityProviderEntity> getIdentityProvider(
            UUID identityProviderKey,
            Map<UUID, Optional<IdentityProviderEntity>> identityProviderCache
    ) throws ValidationException {
        var cachedIdentityProvider = identityProviderCache.get(identityProviderKey);
        if (cachedIdentityProvider != null) {
            return cachedIdentityProvider;
        }

        try {
            var identityProvider = SpringContext
                    .getBean(IdentityProviderService.class)
                    .retrieve(identityProviderKey);

            identityProviderCache.put(identityProviderKey, identityProvider);

            return identityProvider;
        } catch (Exception e) {
            throw new ValidationException(this, "Der ausgewählte Identitätsanbieter konnte nicht überprüft werden.");
        }
    }

    private boolean requiresTrustLevel(IdentityProviderEntity identityProvider) {
        return IDENTITY_PROVIDER_TYPES_REQUIRING_TRUST_LEVEL.contains(identityProvider.getType());
    }

    private boolean hasSelectedTrustLevel(IdentityConfigElementOption option) {
        if (option.getAdditionalScopes() == null) {
            return false;
        }

        return option
                .getAdditionalScopes()
                .stream()
                .anyMatch(StringUtils::isNotNullOrEmpty);
    }

    private boolean hasSelectedOption(IdentityConfigElementSlot slot) {
        if (slot == null || slot.getOptions() == null) {
            return false;
        }

        return slot
                .getOptions()
                .stream()
                .anyMatch(option -> option != null && option.getIdentityProviderKey() != null);
    }

    private boolean hasAcquisitionMethod(IdentityConfigElementSlot slot) {
        return slot != null && (Boolean.TRUE.equals(slot.getAllowsMail()) || hasSelectedOption(slot));
    }
}
