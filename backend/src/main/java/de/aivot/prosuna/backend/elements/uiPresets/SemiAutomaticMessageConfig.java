package de.aivot.prosuna.backend.elements.uiPresets;

import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;

import java.util.List;

public class SemiAutomaticMessageConfig {
    public static void initConfigurationLayout(@Nonnull final GroupLayoutElement layout,
                                               @Nonnull final Integer processId,
                                               @Nonnull final Integer processVersion) {
        layout
                .findChild(LayoutConfig.EXECUTION_TYPE_FIELD_ID, RadioInputElement.class)
                .ifPresent(executionType -> executionType.setOptions(List.of(
                        RadioInputElementOption.of(LayoutConfig.EXECUTION_TYPE_AUTOMATIC, "Automatisch versenden"),
                        RadioInputElementOption.of(LayoutConfig.EXECUTION_TYPE_MANUAL, "Manuell bearbeiten und versenden")
                )));

        layout
                .findChild(AutomaticContent.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> group.setVisibility(createExecutionTypeVisibility(
                        LayoutConfig.EXECUTION_TYPE_AUTOMATIC
                )));

        layout
                .findChild(ManualContent.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> group.setVisibility(createExecutionTypeVisibility(
                        LayoutConfig.EXECUTION_TYPE_MANUAL
                )));

        layout
                .findChild(ManualContent.ASSIGNMENT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(assignment -> {
                    assignment.setAllowedTypes(List.of(
                            AssignmentContextInputElement.ALLOWED_TYPE_ORG_UNIT,
                            AssignmentContextInputElement.ALLOWED_TYPE_TEAM,
                            AssignmentContextInputElement.ALLOWED_TYPE_USER
                    ));
                    assignment.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(processId)
                            .setProcessVersion(processVersion)
                            .setRequiredPermissions(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)));
                });
    }

    public static boolean isAutomatic(@Nonnull final LayoutConfig config) {
        var executionType = StringUtils.toNullableTrimmedString(config.executionType);
        return LayoutConfig.EXECUTION_TYPE_AUTOMATIC.equals(executionType);
    }

    public static boolean isManual(@Nonnull final LayoutConfig config) {
        return !isAutomatic(config);
    }

    @Nonnull
    private static ElementVisibilityFunctions createExecutionTypeVisibility(@Nonnull String executionType) {
        return ElementVisibilityFunctions
                .of(NoCodeExpression.of(
                        NoCodeEqualsOperator.OPERATOR_ID,
                        new NoCodeReference(LayoutConfig.EXECUTION_TYPE_FIELD_ID),
                        new NoCodeStaticValue(executionType)
                ))
                .recalculateReferencedIds();
    }


    public static final String GROUP_ID = "semi_automatic_message_config";

    @LayoutElementPOJOBinding(id = GROUP_ID, type = ElementType.GroupLayout)
    public static class LayoutConfig {
        public static final String EXECUTION_TYPE_FIELD_ID = "execution_type";
        public static final String EXECUTION_TYPE_AUTOMATIC = "automatic";
        public static final String EXECUTION_TYPE_MANUAL = "manual";

        /**
         * Dispatch mode. Only {@link #EXECUTION_TYPE_AUTOMATIC} and {@link #EXECUTION_TYPE_MANUAL} are accepted;
         * missing or unknown values fail execution.
         */
        @InputElementPOJOBinding(id = EXECUTION_TYPE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ausführungsart"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Auswahl, ob die Anforderung automatisch versendet oder vorher durch eine Mitarbeiter:in bearbeitet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String executionType;

        /**
         * Configuration used only for automatic dispatch.
         */
        public AutomaticContent automaticContent;

        /**
         * Configuration used only when a staff member edits and dispatches the message.
         */
        public ManualContent manualContent;
    }

    /**
     * Message templates used for automatic dispatch.
     */
    @LayoutElementPOJOBinding(id = AutomaticContent.GROUP_ID, type = ElementType.GroupLayout)
    public static class AutomaticContent {
        public static final String GROUP_ID = "automatic_group";
        public static final String SUBJECT_FIELD_ID = "automatic_subject";
        public static final String CONTENT_FIELD_ID = "automatic_content";

        /**
         * Subject template rendered against the process data immediately before dispatch.
         */
        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Betreff der Nachricht"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorlage für den Betreff. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String subject;

        /**
         * Rich-text template rendered against the process data immediately before dispatch.
         */
        @InputElementPOJOBinding(id = CONTENT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Nachricht der Aufforderung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorlage für die Nachricht. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String content;
    }

    /**
     * Message templates and assignment used for staff-assisted dispatch.
     */
    @LayoutElementPOJOBinding(id = ManualContent.GROUP_ID, type = ElementType.GroupLayout)
    public static class ManualContent {
        public static final String GROUP_ID = "manual_group";
        public static final String SUBJECT_FIELD_ID = "manual_subject";
        public static final String CONTENT_FIELD_ID = "manual_content";
        public static final String ASSIGNMENT_FIELD_ID = "manual_assignment";

        /**
         * Required subject template rendered once to initialize the editable staff task.
         */
        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorlage für den Betreff"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorbelegung des bearbeitbaren Betreffs. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String subject;

        /**
         * Required rich-text template rendered once to initialize the editable staff task.
         */
        @InputElementPOJOBinding(id = CONTENT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorlage für die Nachricht"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Vorbelegung der bearbeitbaren Nachricht. Unterstützt Template-Tags mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String content;

        /**
         * Staff assignment context used when the node enters manual mode; null or unresolved values fail assignment.
         */
        @InputElementPOJOBinding(id = ASSIGNMENT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der die Aufforderung bearbeiten und versenden darf."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Organisationseinheit, Team oder Mitarbeiter:in suchen"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }
}
