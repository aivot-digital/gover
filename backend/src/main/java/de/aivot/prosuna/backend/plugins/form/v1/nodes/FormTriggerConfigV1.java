package de.aivot.prosuna.backend.plugins.form.v1.nodes;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;

import java.util.List;

@LayoutElementPOJOBinding(id = FormTriggerNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
public class FormTriggerConfigV1 {
    public static final String FORM_SLUG = "formSlug";
    @InputElementPOJOBinding(id = FORM_SLUG, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "URL-Segment des Formulars"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Bestimmt den letzten Teil der öffentlichen Formularadresse. Erlaubt sind Kleinbuchstaben von a bis z, Zahlen und Bindestriche. Über das Kopiersymbol übernehmen Sie die vollständige Adresse."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true)
    })
    public String formSlug;

    public static final String FORM_LAYOUT = "formLayout";
    @InputElementPOJOBinding(id = FORM_LAYOUT, type = ElementType.UiDefinitionInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Formular"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Gestalten Sie die Felder, Hinweise und Eingaberegeln für die ausfüllende Person. Felder mit Datenschlüssel werden beim regulären Abschluss in die Vorgangsdaten übernommen."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "openExternalEditor", boolValue = true),
    })
    public FormLayoutElement formLayout;

    public static final String IDENTITIES = "identities";
    @InputElementPOJOBinding(id = IDENTITIES, type = ElementType.IdentityConfig, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Identitäten"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie bei Bedarf fest, welche Angaben zu Beteiligten erfasst werden und welche Anmeldedienste oder E-Mail-Eingaben dafür zugelassen sind. Eine ausgewählte Identität muss vor der Übermittlung vollständig sein."),
            @ElementPOJOBindingProperty(key = "required", boolValue = false)
    })
    public List<IdentityConfigElementSlot> identities;

    public static final String PAYMENT = "payment";
    @InputElementPOJOBinding(id = PAYMENT, type = ElementType.PaymentConfig, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Zahlung"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Optional: Legen Sie den Zahlungsanbieter und die zu zahlenden Positionen fest. Eine erforderliche Zahlung muss bestätigt sein, bevor der Vorgang über den regulären Ausgang fortgesetzt wird."),
            @ElementPOJOBindingProperty(key = "required", boolValue = false)
    })
    public PaymentConfigElementValue payment;
}
