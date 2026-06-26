package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.models.elements.form.input.IdentityConfigElementSlot;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.enums.ElementType;

import java.util.List;

@LayoutElementPOJOBinding(id = FormTriggerNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
public class FormTriggerConfigV1 {
    public static final String FORM_SLUG = "formSlug";
    @InputElementPOJOBinding(id = FORM_SLUG, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "URL-Segment des Formulars"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Dieses Segment wird an den URL-Namespace des Prozesses angehängt. Der vollständige öffentliche Pfad lautet /form/{prozess-namespace}/{formular-segment}/."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true)
    })
    public String formSlug;

    public static final String FORM_LAYOUT = "formLayout";
    @InputElementPOJOBinding(id = FORM_LAYOUT, type = ElementType.UiDefinitionInput, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Formular"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie das Formular, das über die angegebene URL ausgespielt werden soll."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "openExternalEditor", boolValue = true),
    })
    public FormLayoutElement formLayout;

    public static final String IDENTITIES = "identities";
    @InputElementPOJOBinding(id = IDENTITIES, type = ElementType.IdentityConfig, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Identitäten"),
            @ElementPOJOBindingProperty(key = "hint", strValue = ""),
            @ElementPOJOBindingProperty(key = "required", boolValue = false)
    })
    public List<IdentityConfigElementSlot> identities;
}
