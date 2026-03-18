package de.aivot.GoverBackend.plugins.form.v1.nodes;

import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.form.Form;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.models.*;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FormTriggerNodeV1 implements ProcessNodeDefinition, PluginComponent {
    public static final String NODE_KEY = "form";
    private static final String PORT_NAME = "input";

    public static final String DATA_KEY_PAYLOAD = "payload";
    public static final String DATA_KEY_FORM_ID = "formId";
    public static final String DATA_KEY_FORM_VERSION = "formVersion";
    public static final String DATA_KEY_ATTACHMENTS = "attachments";

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Trigger;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Dateneingang",
                        "Es wurden Daten von einem Gover Formular empfangen."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(context.getThisProcessInstance().getInitialPayload())
                .setProcessData((Map<String, Object>) context.getThisProcessInstance().getInitialPayload().get(DATA_KEY_PAYLOAD));
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Form.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getName() {
        return "Formulareingang";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Wird durch einen Formulareingang ausgelöst";
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        try {
            return ElementPOJOMapper
                    .createFromPOJO(FormTriggerConfig.class);
        } catch (ElementDataConversionException e) {
            throw new RuntimeException(e);
        }
    }


    @LayoutElementPOJOBinding(id = FormTriggerNodeV1.NODE_KEY, type = ElementType.ConfigLayout)
    public static class FormTriggerConfig {
        public static final String FORM_ID = "formId";
        @InputElementPOJOBinding(id = FORM_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Absendendes Formular"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Das absendende Formular, dessen Eingang diesen Trigger auslösen soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String formId;
    }
}
