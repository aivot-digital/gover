package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.enums.ElementType;

@LayoutElementPOJOBinding(id = FormTriggerNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
public class FormTriggerConfigV1 {
    public static final String FORM_SLUG = "formSlug";
    @InputElementPOJOBinding(id = FORM_SLUG, type = ElementType.Text, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Formular-URL"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Die URL, über die das Formular angesprochen werden kann."),
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

    /*
    public static final String IDENTITY_PROVIDERS = "identityProviders";
    public List<IdentityProviderConfig> identityProviders;

    @ReplicatingContainerLayoutElementElementPOJOBinding(id = IDENTITY_PROVIDERS, properties = {
            @ElementPOJOBindingProperty(key = "label", strValue = "Identitätsanbieter"),
            @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie alle Identitätsanbieter, die in diesem Formular zur Verfügung stehen."),
            @ElementPOJOBindingProperty(key = "required", boolValue = true),
            @ElementPOJOBindingProperty(key = "headlineTemplate", strValue = "#. Identitätsanbieter"),
            @ElementPOJOBindingProperty(key = "addLabel", strValue = "Neuer Identitätsanbieter"),
            @ElementPOJOBindingProperty(key = "removeLabel", strValue = "Identitätsanbieter entfernen")
    })
    public static class IdentityProviderConfig {
    }
     */
}
