package de.aivot.prosuna.backend.plugins.core.v1.communication;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderContext;
import de.aivot.prosuna.backend.communication.models.CommunicationProviderDefinition;
import de.aivot.prosuna.backend.communication.services.DefaultMailCommunicationService;
import de.aivot.prosuna.backend.communication.utils.EmailAddressUtils;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElementPattern;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityProviderType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class MailCommunicationProviderV1 implements CommunicationProviderDefinition<MailCommunicationProviderV1.Config, MailCommunicationProviderV1.IdentityBinding> {
    public static final String COMPONENT_KEY = "mail_communication_provider";
    public static final String EMAIL_ATTRIBUTE_FIELD_ID = "emailAttribute";
    public static final String CUSTOMER_EMAIL_FIELD_ID = "email";

    private final DefaultMailCommunicationService defaultMailCommunicationService;

    public MailCommunicationProviderV1(DefaultMailCommunicationService defaultMailCommunicationService) {
        this.defaultMailCommunicationService = defaultMailCommunicationService;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return COMPONENT_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "E-Mail-Kommunikation";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Versendet Nachrichten per E-Mail an die ausgewählte Identität.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Versendet Nachrichten an eine E-Mail-Adresse aus den Daten der ausgewählten Identität oder an eine von der Kund:in ergänzte Adresse.";
    }

    @Nonnull
    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigLayout() {
        var layout = new ConfigLayoutElement();
        layout.setId("mail-provider-config");
        layout.setChildren(List.of());
        return layout;
    }

    @Nonnull
    @Override
    public List<IdentityProviderType> getSupportedIdentityProviderTypes() {
        return Arrays.asList(IdentityProviderType.values());
    }

    @Nonnull
    @Override
    public Class<IdentityBinding> getIdentityProviderBindingConfigClass() {
        return IdentityBinding.class;
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getIdentityProviderBindingConfigLayout(@Nonnull IdentityProviderEntity identityProviderEntity) {
        var attributeOptions = identityProviderEntity.getAttributes() == null
                ? List.<SelectInputElementOption>of()
                : identityProviderEntity.getAttributes()
                .stream()
                .filter(attribute -> attribute.getKeyInData() != null && !attribute.getKeyInData().isBlank())
                .map(attribute -> SelectInputElementOption.of(
                        attribute.getKeyInData(),
                        attribute.getLabel() == null || attribute.getLabel().isBlank()
                                ? attribute.getKeyInData()
                                : attribute.getLabel()
                ))
                .toList();

        var emailAttribute = new SelectInputElement();
        emailAttribute.setId(EMAIL_ATTRIBUTE_FIELD_ID);
        emailAttribute.setLabel("E-Mail-Attribut");
        emailAttribute.setHint("Optionales Attribut des Nutzerkontenanbieters. Fehlt es in den Anmeldedaten, wird die E-Mail-Adresse von der Kund:in abgefragt.");
        emailAttribute.setRequired(false);
        emailAttribute.setOptions(attributeOptions);

        var layout = new ConfigLayoutElement();
        layout.setId("mail-identity-provider-binding-config");
        layout.setChildren(List.of(emailAttribute));
        return layout;
    }

    @Override
    public GroupLayoutElement getCustomerLayout(
            @Nonnull CommunicationProviderContext<Config, IdentityBinding> context,
            @Nonnull IdentityData identityData
    ) {
        if (resolveMappedEmail(context.identityProviderBindingConfiguration(), identityData) != null) {
            return null;
        }

        var email = new TextInputElement();
        email.setId(CUSTOMER_EMAIL_FIELD_ID);
        email.setLabel("E-Mail-Adresse");
        email.setHint("An diese Adresse werden Nachrichten zu Ihrem Vorgang gesendet.");
        email.setAutocomplete("email");
        email.setRequired(true);
        email.setPattern(TextInputElementPattern.of(
                EmailAddressUtils.EMAIL_PATTERN_VALUE,
                "Bitte geben Sie eine gültige E-Mail-Adresse ein."
        ));

        var layout = new GroupLayoutElement();
        layout.setId("mail-customer-config");
        layout.setChildren(List.of(email));
        return layout;
    }

    @Override
    public Map<String, Object> sendMessage(@Nonnull CommunicationProviderContext<Config, IdentityBinding> context,
                                           @Nonnull IdentityData identity,
                                           @Nonnull CommunicationMessage message) throws CommunicationException {
        var recipient = resolveEmail(context.identityProviderBindingConfiguration(), identity);
        if (recipient == null) {
            throw new CommunicationException("Für die Identität %s ist keine E-Mail-Adresse verfügbar.", identity.identityId());
        }
        defaultMailCommunicationService.sendMessage(recipient, message);

        assert message.subject() != null;
        assert message.body() != null;

        return Map.of(
                "recipient", recipient,
                "subject", message.subject(),
                "body", message.body()
        );
    }

    private static String resolveEmail(@Nonnull IdentityBinding identityBinding, @Nonnull IdentityData identity) {
        var mappedEmail = resolveMappedEmail(identityBinding, identity);
        if (mappedEmail != null) {
            return mappedEmail;
        }
        var customerEmail = identity.communicationProviderData().get(CUSTOMER_EMAIL_FIELD_ID);
        if (customerEmail instanceof String value && !value.isBlank()) {
            return value.trim();
        }
        return null;
    }

    private static String resolveMappedEmail(@Nonnull IdentityBinding identityBinding, @Nonnull IdentityData identity) {
        if (identityBinding.emailAttribute != null && !identityBinding.emailAttribute.isBlank()) {
            var mapped = identity.attributes().get(identityBinding.emailAttribute);
            if (mapped != null) {
                var trimmed = mapped.trim();
                if (EmailAddressUtils.isValidSingleAddress(trimmed)) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    @LayoutElementPOJOBinding(id = "mail-provider-config", type = ElementType.ConfigLayout)
    public static class Config {
    }

    @LayoutElementPOJOBinding(id = "mail-identity-provider-binding-config", type = ElementType.ConfigLayout)
    public static class IdentityBinding {
        @InputElementPOJOBinding(id = EMAIL_ATTRIBUTE_FIELD_ID, type = ElementType.Select)
        public String emailAttribute;
    }
}
