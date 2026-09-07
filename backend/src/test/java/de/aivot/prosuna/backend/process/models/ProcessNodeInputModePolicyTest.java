package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.InputVariableSource;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.plugins.ai.v1.nodes.AiCompletionActionNodeV1;
import de.aivot.prosuna.backend.plugins.ai.v1.nodes.AiProcessDataTransformationActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.ApprovalActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.CounterActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.DataChangeActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.DataMappingActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.EMailActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.LowCodeActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.ManualActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.NoCodeActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.PaymentRequestActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.PdfActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.WriteExternalStorageActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.DataTypeValidationControlNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.flow.IfFlowControlNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.terminators.DefaultTerminationNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.triggers.webhook.WebhookTriggerConfigV1;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerConfigV1;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProcessNodeInputModePolicyTest {
    private static final List<InputMode> ALL_INPUT_MODES = List.of(
            InputMode.Literal,
            InputMode.Variable,
            InputMode.NoCode,
            InputMode.LowCode
    );

    @Test
    void runtimeValueFields_ShouldExposeAllInputModesAndVariableSources() throws Exception {
        assertDynamicFields(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.class, "model", "prompt");
        assertDynamicFields(AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.class, "model", "prompt");
        assertDynamicFields(ApprovalActionNodeV1.ApprovalConfiguration.class, "criteria", "customContent");
        assertDynamicFields(
                EMailActionNodeV1.EMailActionNodeConfig.class,
                "to",
                "bcc",
                "manual_subject",
                "manual_content",
                "automatic_subject",
                "automatic_content"
        );
        assertDynamicFields(ManualActionNodeV1.ManualActionNodeConfig.class, "task_description");
        assertDynamicFields(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.class, "recipientEmail");
        assertDynamicFields(CounterActionNodeV1.CounterActionNodeV1Configuration.class, "increment");
        assertDynamicFields(
                WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig.class,
                "attachment_sets",
                "file_name"
        );
        assertDynamicFields(
                DataTypeValidationControlNodeV1.DataTypeValidationControlNodeConfig.class,
                "rules",
                "path",
                "expectedType"
        );
    }

    @Test
    void structuralAndSecurityFields_ShouldRemainLiteralOnly() throws Exception {
        // These values influence editor structure, metadata, credentials, assignments or durable data destinations.
        // In particular, the AI endpoint and secret are needed during authoring to load the literal model options.
        assertLiteralOnlyFields(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.class, "endpointUrl", "apiKeySecret");
        assertLiteralOnlyFields(ApprovalActionNodeV1.ApprovalConfiguration.class, "contentMode", "dataContent", "assignmentContext");
        assertLiteralOnlyFields(
                EMailActionNodeV1.EMailActionNodeConfig.class,
                "attachment_file_names",
                "execution_type",
                "manual_assignment"
        );
        assertLiteralOnlyFields(ManualActionNodeV1.ManualActionNodeConfig.class, "ui_definition", "assignment_context");
        assertLiteralOnlyFields(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.class, "payment");
        assertLiteralOnlyFields(CounterActionNodeV1.CounterActionNodeV1Configuration.class, "variable");
        assertLiteralOnlyFields(
                WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig.class,
                "attachment_set_data_keys",
                "storage_path"
        );
        assertLiteralOnlyFields(
                PdfActionNodeV1.PdfActionNodeConfig.class,
                "file_name",
                "content_html_source",
                "content_html_code",
                "content_html_asset_key"
        );

        // Data destinations and rule definitions describe the process data shape available to later nodes.
        assertLiteralOnlyFields(
                DataMappingActionNodeV1.DataMappingActionNodeV1Config.class,
                "cleanupEmptyContainers",
                "rules",
                "source",
                "cleanupSource",
                "deleteOnly",
                "target"
        );
        assertLiteralOnlyFields(
                NoCodeActionNodeV1.NoCodeActionNodeConfiguration.class,
                "variables",
                "name",
                "targetType",
                "expression"
        );

        // Dedicated expression nodes already model their dynamic behavior and must not be wrapped in another mode.
        assertLiteralOnlyFields(LowCodeActionNodeV1.LowCodeActionNodeConfig.class, "js_code");
        assertLiteralOnlyFields(
                IfFlowControlNodeV1.IfFlowControlNodeConfig.class,
                "conditionType",
                "condition",
                "conditionNoCode"
        );

        // Trigger routing and retention are operational configuration, not values derived from running process data.
        assertLiteralOnlyFields(
                WebhookTriggerConfigV1.class,
                "request_method",
                "slug",
                "auth_required",
                "auth_method",
                "auth_username",
                "auth_password",
                "auth_token",
                "request_body_type",
                "processing_type",
                "processing_code",
                "copy_to_process_data"
        );
        assertLiteralOnlyFields(FormTriggerConfigV1.class, "formSlug", "formLayout", "identities", "payment");
        assertLiteralOnlyFields(
                DefaultTerminationNodeV1.DefaultTerminationNodeV1Config.class,
                "retention_value",
                "retention_unit"
        );
        assertLiteralOnlyFields(
                DataChangeActionNodeV1.DataChangeActionNodeConfig.class,
                "data_definition",
                "assignment_context"
        );
    }

    @Test
    void textTemplates_ShouldExposeDynamicTextIndependentlyFromInputModes() throws Exception {
        assertDynamicTextFields(AiCompletionActionNodeV1.AiCompletionActionNodeConfig.class, "prompt");
        assertDynamicTextFields(AiProcessDataTransformationActionNodeV1.AiProcessDataTransformationActionNodeConfig.class, "prompt");
        assertDynamicTextFields(ApprovalActionNodeV1.ApprovalConfiguration.class, "criteria", "customContent");
        assertDynamicTextFields(
                EMailActionNodeV1.EMailActionNodeConfig.class,
                "to",
                "bcc",
                "manual_subject",
                "manual_content",
                "automatic_subject",
                "automatic_content"
        );
        assertDynamicTextFields(ManualActionNodeV1.ManualActionNodeConfig.class, "task_description");
        assertDynamicTextFields(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.class, "recipientEmail");
        assertDynamicTextFields(PdfActionNodeV1.PdfActionNodeConfig.class, "file_name");
        assertDynamicTextFields(WriteExternalStorageActionNodeV1.WriteExternalStorageActionNodeConfig.class, "file_name");
    }

    private static void assertDynamicFields(Class<?> configurationClass, String... fieldIds) throws Exception {
        var layout = ElementPOJOMapper.createFromPOJO(configurationClass);

        for (var fieldId : fieldIds) {
            var input = assertInstanceOf(BaseInputElement.class, layout.findChild(fieldId).orElseThrow());
            assertEquals(ALL_INPUT_MODES, input.getInputModePolicy().allowedModes(), fieldId);
            assertEquals(InputMode.Literal, input.getInputModePolicy().effectiveDefaultMode(), fieldId);
            assertEquals(List.of(InputVariableSource.values()), input.getInputModePolicy().allowedVariableSources(), fieldId);
        }
    }

    private static void assertLiteralOnlyFields(Class<?> configurationClass, String... fieldIds) throws Exception {
        var layout = ElementPOJOMapper.createFromPOJO(configurationClass);

        for (var fieldId : fieldIds) {
            var input = assertInstanceOf(BaseInputElement.class, layout.findChild(fieldId).orElseThrow());
            assertNull(input.getInputModePolicy(), fieldId);
        }
    }

    private static void assertDynamicTextFields(Class<?> configurationClass, String... fieldIds) throws Exception {
        var layout = ElementPOJOMapper.createFromPOJO(configurationClass);

        for (var fieldId : fieldIds) {
            var input = assertInstanceOf(BaseInputElement.class, layout.findChild(fieldId).orElseThrow());
            assertEquals(
                    List.of(InputVariableSource.values()),
                    input.getDynamicTextPolicy().variableSuggestionSources(),
                    fieldId
            );
        }
    }
}
