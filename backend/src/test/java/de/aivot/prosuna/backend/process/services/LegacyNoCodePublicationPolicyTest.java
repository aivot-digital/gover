package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessRetentionTimeUnit;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessVersionRepository;
import de.aivot.prosuna.backend.system.properties.BuildProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LegacyNoCodePublicationPolicyTest {
    private LegacyNoCodePublicationPolicy policy(String release) {
        var properties = new BuildProperties();
        properties.setBuildVersion(release);
        return new LegacyNoCodePublicationPolicy(properties, JsonMapperTestUtils.createMapper());
    }

    private ProcessNodeEntity legacyNode() {
        return new ProcessNodeEntity().setId(23).setName("Antrag").setConfiguration(new AuthoredElementValues()
                .putLiteral("form", Map.of("children", List.of(Map.of("id", "age", "type", 2, "label", "Alter",
                        "visibility", Map.of("conditionSet", Map.of("conditions", List.of())),
                        "validation", Map.of("type", "ConditionSet", "conditionSet", Map.of("conditions", List.of())))))));
    }

    @ParameterizedTest
    @CsvSource({"5.0.0,false", "5.0.99,false", "5.1.0,true", "5.1.0-SNAPSHOT,true", "v5.1.0-rc.1,true", "5.2.3+build,true", "6.0.0,true", "0.0.0,false", "@buildVersion,false"})
    void enforcesOnlyFrom51(String release, boolean expected) {
        assertEquals(expected, policy(release).isEnforced());
    }

    @Test
    void leavesUnversionedDevelopmentBuildsInTransitionPeriod() {
        assertFalse(policy(null).isEnforced());
    }

    @Test
    void reportsImplicitAndExplicitNestedRulesOnceWithLocationsWithoutMutatingConfiguration() {
        var node = legacyNode();
        var before = JsonMapperTestUtils.createMapper().writeValueAsString(node.getConfiguration());
        var problems = policy("5.1.0").findProblems(node);
        assertEquals(2, problems.size());
        assertTrue(problems.stream().allMatch(problem -> problem.contains("Antrag") && problem.contains("ID 23") && problem.contains("Alter") && problem.contains("ID age")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("Sichtbarkeit") && problem.contains("Konfiguration.form.value.children[0].visibility.conditionSet")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("Validierung")));
        assertEquals(before, JsonMapperTestUtils.createMapper().writeValueAsString(node.getConfiguration()));
    }

    @Test
    void detectsTaggedFunctionInputButIgnoresBusinessPropertiesAndStrings() {
        var node = new ProcessNodeEntity().setId(23).setName("Antrag").setConfiguration(new AuthoredElementValues()
                .putLiteral("custom", Map.of("conditionSet", Map.of()))
                .putLiteral("text", "{\"type\":\"ConditionSet\",\"conditionSet\":{}}")
                .putLiteral("expression", Map.of("type", "NoCode", "noCode", Map.of("type", "NoCodeStaticValue", "value", true))));
        assertTrue(policy("5.1.0").findProblems(node).isEmpty());
        node.getConfiguration().putLiteral("function", Map.of("type", "ConditionSet", "conditionSet", Map.of()));
        assertEquals(1, policy("5.1.0").findProblems(node).size());
    }

    @ParameterizedTest
    @EnumSource(value = ProcessVersionStatus.class, names = {"Drafted", "Revoked"})
    void blocksInitialAndRepeatedPublicationBeforeChangingStatusOrSaving(ProcessVersionStatus status) {
        var repository = mock(ProcessVersionRepository.class);
        var nodes = mock(ProcessNodeService.class);
        when(nodes.findAllByProcessIdAndProcessVersion(12, 3)).thenReturn(List.of(legacyNode()));
        var service = new ProcessVersionService(repository, mock(ProcessRepository.class), nodes,
                mock(ProcessNodeDefinitionService.class), mock(CaseNumberGeneratorService.class), policy("5.1.0"));
        var version = version(status);
        var error = assertThrows(ResponseException.class, () -> service.updateStatus(version, ProcessVersionStatus.Published));
        assertEquals(HttpStatus.NOT_ACCEPTABLE, error.getStatus());
        assertTrue(error.getMessage().contains("Prozess 12, Version 3"));
        assertTrue(error.getMessage().contains("Sichtbarkeit"));
        assertEquals(status, version.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void allowsLegacyPublicationIn50AndRevocationIn51WithoutInspectingRuntimeRules() throws ResponseException {
        for (var release : List.of("5.0.0", "5.1.0")) {
            var repository = mock(ProcessVersionRepository.class);
            when(repository.save(any(ProcessVersionEntity.class))).thenAnswer(call -> call.getArgument(0));
            var nodes = mock(ProcessNodeService.class);
            var service = new ProcessVersionService(repository, mock(ProcessRepository.class), nodes,
                    mock(ProcessNodeDefinitionService.class), mock(CaseNumberGeneratorService.class), policy(release));
            var initial = release.equals("5.0.0") ? ProcessVersionStatus.Drafted : ProcessVersionStatus.Published;
            var target = release.equals("5.0.0") ? ProcessVersionStatus.Published : ProcessVersionStatus.Revoked;
            var version = version(initial);
            assertEquals(target, service.updateStatus(version, target).getStatus());
            verify(repository).save(version);
            verifyNoInteractions(nodes);
        }
    }

    @Test
    void allowsPublicationWithoutLegacyRulesIn51() throws ResponseException {
        var repository = mock(ProcessVersionRepository.class);
        when(repository.save(any(ProcessVersionEntity.class))).thenAnswer(call -> call.getArgument(0));
        var nodes = mock(ProcessNodeService.class);
        when(nodes.findAllByProcessIdAndProcessVersion(12, 3)).thenReturn(List.of(new ProcessNodeEntity()
                .setId(23).setName("Antrag").setConfiguration(new AuthoredElementValues())));
        var service = new ProcessVersionService(repository, mock(ProcessRepository.class), nodes,
                mock(ProcessNodeDefinitionService.class), mock(CaseNumberGeneratorService.class), policy("5.1.0"));
        assertEquals(ProcessVersionStatus.Published, service.updateStatus(version(ProcessVersionStatus.Drafted), ProcessVersionStatus.Published).getStatus());
    }

    @ParameterizedTest
    @CsvSource({"5.0.0,false", "5.1.0,true"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    void exposesConcreteLocationsInVersionValidationFrom51(String release, boolean expected) throws ResponseException {
        var processes = mock(ProcessRepository.class);
        when(processes.findById(12)).thenReturn(java.util.Optional.of(new de.aivot.prosuna.backend.process.entities.ProcessEntity().setId(12)));
        var nodes = mock(ProcessNodeService.class);
        when(nodes.findAllByProcessIdAndProcessVersion(12, 3)).thenReturn(List.of(legacyNode()));
        var definitions = mock(ProcessNodeDefinitionService.class);
        when(definitions.getProcessNodeDefinition(any(ProcessNodeEntity.class))).thenReturn(java.util.Optional.of(mock(de.aivot.prosuna.backend.process.models.ProcessNodeDefinition.class)));
        var service = new ProcessVersionService(mock(ProcessVersionRepository.class), processes, nodes, definitions,
                mock(CaseNumberGeneratorService.class), policy(release));
        var problems = service.validate(version(ProcessVersionStatus.Drafted));
        assertEquals(expected, problems.versionProblems().stream().anyMatch(problem -> problem.contains("Sichtbarkeit") && problem.contains("Konfiguration.form.value.children[0]")));
    }

    private ProcessVersionEntity version(ProcessVersionStatus status) {
        return new ProcessVersionEntity().setProcessId(12).setProcessVersion(3).setStatus(status)
                .setRetentionTimeValue(30).setRetentionTimeUnit(ProcessRetentionTimeUnit.Days);
    }
}
