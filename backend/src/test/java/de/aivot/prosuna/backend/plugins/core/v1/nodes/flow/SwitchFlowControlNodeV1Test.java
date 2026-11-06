package de.aivot.prosuna.backend.plugins.core.v1.nodes.flow;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.NoCodeInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.services.InputVariableResolver;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.prosuna.backend.plugins.core.v1.operators.CommonOperatorsV1;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeConfigurationValidationPhase;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwitchFlowControlNodeV1Test {
    private static final String ID_1 = "0190a6f2-1a2b-7c3d-8e4f-000000000001";
    private static final String ID_2 = "0190a6f2-1a2b-7c3d-8e4f-000000000002";
    private static final String ID_3 = "0190a6f2-1a2b-7c3d-8e4f-000000000003";
    private static final String ID_4 = "0190a6f2-1a2b-7c3d-8e4f-000000000004";

    private final SwitchFlowControlNodeV1 node = new SwitchFlowControlNodeV1(
            new JavascriptEngineFactoryService(List.of()),
            new NoCodeEvaluationService(List.of())
    );

    @Test
    void shouldShowOnlyTheEditorOfTheSelectedConditionTypeInEachCase() throws Exception {
        var cases = layout().findChild(SwitchFlowControlNodeV1.CASES_FIELD_ID, ReplicatingContainerLayoutElement.class).orElseThrow();

        assertEquals(Boolean.TRUE, cases.getRequired());
        assertNotNull(cases.findChild(FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID, CodeInputElement.class).orElseThrow().getVisibility());
        assertNotNull(cases.findChild(FlowConditionSupport.CONDITION_NO_CODE_FIELD_ID, NoCodeInputElement.class).orElseThrow().getVisibility());
    }

    @Test
    void shouldDerivePortKeysAndConditionEditorVisibilityFromAuthoredRows() throws Exception {
        var lowCodeRow = new AuthoredElementValues()
                .putLiteral(SwitchFlowControlNodeV1.CASE_LABEL_FIELD_ID, "Antrag")
                .putLiteral(FlowConditionSupport.CONDITION_TYPE_FIELD_ID, FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE)
                .putLiteral(FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID, "true");
        var noCodeRow = new AuthoredElementValues()
                .putLiteral(SwitchFlowControlNodeV1.CASE_LABEL_FIELD_ID, "Bescheid")
                .putLiteral(FlowConditionSupport.CONDITION_TYPE_FIELD_ID, FlowConditionSupport.CONDITION_TYPE_VALUE_NO_CODE);
        var authored = new AuthoredElementValues().putLiteral(SwitchFlowControlNodeV1.CASES_FIELD_ID, List.of(
                new ReplicatingContainerLayoutElementValue().setId(ID_1).setValues(lowCodeRow),
                new ReplicatingContainerLayoutElementValue().setId(ID_2).setValues(noCodeRow)
        ));

        var derived = derivationService().derive(layout(), authored);
        var config = ElementPOJOMapper.mapToPOJO(derived.getEffectiveValues(), SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig.class);

        assertEquals(List.of("0190a6f21a2b7c3d8e4f000000000001", "0190a6f21a2b7c3d8e4f000000000002", "default"),
                node.getPorts(config).stream().map(ProcessNodePort::key).toList());
        var rows = derived.getElementStates().get(SwitchFlowControlNodeV1.CASES_FIELD_ID).getSubStates();
        assertEquals(Boolean.TRUE, rows.get(0).getStates().get(FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID).getVisible());
        assertEquals(Boolean.FALSE, rows.get(0).getStates().get(FlowConditionSupport.CONDITION_NO_CODE_FIELD_ID).getVisible());
        assertEquals(Boolean.FALSE, rows.get(1).getStates().get(FlowConditionSupport.CONDITION_LOW_CODE_FIELD_ID).getVisible());
        assertEquals(Boolean.TRUE, rows.get(1).getStates().get(FlowConditionSupport.CONDITION_NO_CODE_FIELD_ID).getVisible());
    }

    @Test
    void shouldOfferOnePortPerCaseInOrderAndTheDefaultPortLast() {
        var config = config(null, noCode(ID_1, "Antrag", false), noCode(ID_2, " ", false));

        var ports = node.getPorts(config);

        assertEquals(List.of("0190a6f21a2b7c3d8e4f000000000001", "0190a6f21a2b7c3d8e4f000000000002", "default"),
                ports.stream().map(ProcessNodePort::key).toList());
        assertEquals(List.of("Antrag", "Fall 2", "Standard"), ports.stream().map(ProcessNodePort::label).toList());
        assertTrue(ports.stream().allMatch(port -> port.key().length() <= 32));
    }

    @Test
    void shouldUseConfiguredDefaultPortLabel() {
        var ports = node.getPorts(config("  Sonst  ", noCode(ID_1, "Antrag", false)));

        assertEquals("Sonst", ports.getLast().label());
    }

    @Test
    void shouldContinueViaFirstTrueCaseWithoutEvaluatingLaterCases() throws Exception {
        var result = execute(config(null,
                noCode(ID_1, "Erster", false),
                lowCode(ID_2, "Zweiter", "1 + 1 === 2"),
                noCode(ID_3, "Dritter", true),
                lowCode(ID_4, "Vierter", "throw new Error('darf nicht ausgewertet werden')")
        ));

        assertEquals("0190a6f21a2b7c3d8e4f000000000002", result.getViaPort());
        assertEquals(ID_2, result.getNodeData().get("matchedCaseId"));
        assertEquals("Zweiter", result.getNodeData().get("matchedCaseLabel"));
        assertEquals(false, result.getNodeData().get("defaultUsed"));
    }

    @Test
    void shouldContinueViaDefaultPortWhenNoCaseIsTrue() throws Exception {
        var result = execute(config(null, noCode(ID_1, "Erster", false), lowCode(ID_2, "Zweiter", "false")));

        assertEquals("default", result.getViaPort());
        assertNull(result.getNodeData().get("matchedCaseId"));
        assertNull(result.getNodeData().get("matchedCaseLabel"));
        assertEquals(true, result.getNodeData().get("defaultUsed"));
    }

    @Test
    void shouldRejectNonBooleanLowCodeResults() {
        assertThrows(ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> execute(config(null, lowCode(ID_1, "Erster", "'ja'"))));
    }

    @Test
    void shouldRejectCaseLabelsMatchingTheEffectiveDefaultLabel() {
        var errors = validate(config(null, noCode(ID_1, " standard ", true)));

        assertEquals(List.of("Fall 1: Die Beschriftung „standard“ wird bereits für den Standardausgang verwendet. Vergeben Sie eine eindeutige Beschriftung."),
                errors);
        assertNotNull(validate(config("Sonst", noCode(ID_1, "SONST", true))));
    }

    @Test
    void shouldAllowStandardAsCaseLabelWhenDefaultLabelIsCustomized() {
        assertNull(validate(config("Sonst", noCode(ID_1, "Standard", true))));
    }

    @Test
    void shouldRejectDuplicateCaseLabels() {
        var errors = validate(config(null, noCode(ID_1, "Antrag", true), noCode(ID_2, "Bescheid", true), noCode(ID_3, " ANTRAG", true)));

        assertEquals(List.of("Fall 3: Die Beschriftung „ANTRAG“ wird bereits für Fall 1 verwendet. Vergeben Sie eine eindeutige Beschriftung."),
                errors);
    }

    private ConfigLayoutElement layout() throws Exception {
        return node.getConfigurationLayout(new ProcessNodeDefinitionConfigurationLayoutContext(
                null, new ProcessEntity(), new ProcessVersionEntity(), new ProcessNodeEntity()));
    }

    private static ElementDerivationService derivationService() {
        return new ElementDerivationService(
                new JavascriptEngineFactoryService(List.of()),
                // The visibility rules of the condition editors need the common operators.
                new NoCodeEvaluationService(List.of(new CommonOperatorsV1(null, null, new BusinessTime(ZoneId.of("Europe/Berlin"), Clock.systemUTC())))),
                new ElementDataTransformService(),
                new CodeListElementOptionsService(null, null),
                new AuthoredInputValueService(JsonMapperTestUtils.createMapper()),
                new InputVariableResolver()
        );
    }

    private List<String> validate(SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig config) {
        var errors = node.validateConfiguration(new ProcessNodeConfigurationValidationContext<>(
                new ProcessNodeEntity(), config, DerivedRuntimeElementData.empty(), ProcessNodeConfigurationValidationPhase.Authoring
        ));
        return errors == null ? null : errors.get(SwitchFlowControlNodeV1.CASES_FIELD_ID);
    }

    private ProcessNodeExecutionResultTaskCompleted execute(SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig config) throws Exception {
        ProcessNodeExecutionInitContext<SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig> context =
                mock(ProcessNodeExecutionInitContext.class);
        when(context.getCurrentProcessExecutionData()).thenReturn(new ProcessExecutionData());
        when(context.getConfigurationOfExecutingNode()).thenReturn(config);
        return assertInstanceOf(ProcessNodeExecutionResultTaskCompleted.class, node.init(context));
    }

    private static SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig config(String defaultPortLabel,
                                                                             SwitchFlowControlNodeV1.SwitchCaseConfig... cases) {
        var config = new SwitchFlowControlNodeV1.SwitchFlowControlNodeConfig();
        config.defaultPortLabel = defaultPortLabel;
        config.cases = List.of(cases);
        return config;
    }

    private static SwitchFlowControlNodeV1.SwitchCaseConfig noCode(String id, String label, boolean value) {
        var caseConfig = caseConfig(id, label, FlowConditionSupport.CONDITION_TYPE_VALUE_NO_CODE);
        caseConfig.conditionNoCode = new NoCodeInputElementItem(new NoCodeStaticValue(value));
        return caseConfig;
    }

    private static SwitchFlowControlNodeV1.SwitchCaseConfig lowCode(String id, String label, String code) {
        var caseConfig = caseConfig(id, label, FlowConditionSupport.CONDITION_TYPE_VALUE_LOW_CODE);
        caseConfig.condition = code;
        return caseConfig;
    }

    private static SwitchFlowControlNodeV1.SwitchCaseConfig caseConfig(String id, String label, String conditionType) {
        var caseConfig = new SwitchFlowControlNodeV1.SwitchCaseConfig();
        caseConfig.id = id;
        caseConfig.caseLabel = label;
        caseConfig.conditionType = conditionType;
        return caseConfig;
    }
}
