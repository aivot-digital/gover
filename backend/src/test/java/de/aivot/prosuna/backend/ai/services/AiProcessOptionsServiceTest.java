package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.dataObject.services.*;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.services.PaymentProviderService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.models.ProcessInstanceAccessSelectableItem;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.secrets.entities.SecretEntity;
import de.aivot.prosuna.backend.secrets.permissions.SecretPermissionProvider;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageProviderService;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import de.aivot.prosuna.backend.teams.filters.TeamFilter;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.teams.services.TeamService;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiProcessOptionsServiceTest {
    private final PermissionService permissions = mock(PermissionService.class);
    private final PotentialProcessInstanceAccessService access = mock(PotentialProcessInstanceAccessService.class);
    private final TeamService teams = mock(TeamService.class);
    private final SecretService secrets = mock(SecretService.class);
    private final CodeListElementOptionsService codeLists = mock(CodeListElementOptionsService.class);
    private final AiProcessChatContext scope = new AiProcessChatContext("owner", "session", 7, 2);
    private final ProcessNodeEntity node = new ProcessNodeEntity();
    private final tools.jackson.databind.json.JsonMapper mapper = JsonMapperTestUtils.createMapper();
    private AiProcessOptionsService service;

    @BeforeEach
    void setUp() throws Exception {
        when(codeLists.resolve(any())).thenAnswer(i -> i.getArgument(0));
        service = new AiProcessOptionsService(mock(de.aivot.prosuna.backend.asset.services.VStorageIndexItemWithAssetService.class), mock(DataObjectSchemaService.class), mock(DataObjectItemService.class), permissions, access, teams,
                mock(VDepartmentShadowedService.class), mock(UserService.class), secrets, mock(StorageProviderService.class),
                mock(IdentityProviderService.class), mock(PaymentProviderService.class), mock(ProcessNodeService.class), codeLists, List.of(), List.of(), mapper);
    }

    @Test
    void restrictsTeamQueryToGrantedIdsAndHonorsGlobalOverride() throws Exception {
        var field = new DomainAndUserSelectInputElement().setAllowedTypes(List.of("team"));
        when(permissions.getTeamsWithPermission("owner", TeamPermissionProvider.TEAM_READ)).thenReturn(List.of(17));
        when(teams.list(any(), any(TeamFilter.class))).thenReturn(new PageImpl<>(List.of(new TeamEntity().setId(17).setName("Team"))));
        service.options(scope, node, field, null, null, null);
        verify(teams).list(any(), argThat((TeamFilter filter) -> filter.getIds().equals(List.of(17))));
        when(permissions.hasSystemPermission("owner", TeamPermissionProvider.TEAM_READ)).thenReturn(true);
        service.options(scope, node, field, null, null, null);
        verify(teams).list(any(), argThat((TeamFilter filter) -> filter.getIds() == null));
    }

    @Test
    void enforcesProcessConstrainedSelectionAndAllowedKinds() throws Exception {
        var field = new AssignmentContextInputElement().setAllowedTypes(List.of("team"))
                .setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint().setProcessId(7).setProcessVersion(2).setRequiredPermissions(List.of("task.read")));
        when(access.listSelectableItems(7, 2, List.of("task.read"))).thenReturn(List.of(
                new ProcessInstanceAccessSelectableItem("team", "17", "Team", null, null, 1),
                new ProcessInstanceAccessSelectableItem("user", "abc", "Person", null, null, null)));
        var result = mapper.valueToTree(service.options(scope, node, field, null, null, null));
        assertThat(result.path("items")).hasSize(1);
        assertThat(result.at("/items/0/value/type").asString()).isEqualTo("team");
        verify(permissions).requireProcessPermission("owner", 7, ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE);
        field.getProcessAccessConstraint().setProcessId(8);
        assertThatThrownBy(() -> service.options(scope, node, field, null, null, null)).isInstanceOf(ResponseException.class);
    }

    @Test
    void returnsOnlySecretMetadataAndNeverQueriesWithoutPermission() throws Exception {
        var secret = new SecretEntity();
        secret.setKey(UUID.randomUUID());
        secret.setName("API credential");
        when(secrets.list(any(), any())).thenReturn(new PageImpl<>(List.of(secret)));
        var result = mapper.valueToTree(service.options(scope, node, new SecretSelectInputElement(), null, null, null));
        assertThat(result.at("/items/0").propertyNames()).containsExactlyInAnyOrder("value", "label");
        verify(permissions).requireSystemPermission("owner", SecretPermissionProvider.SECRET_READ);
        clearInvocations(secrets);
        doThrow(ResponseException.forbidden()).when(permissions).requireSystemPermission("owner", SecretPermissionProvider.SECRET_READ);
        assertThatThrownBy(() -> service.options(scope, node, new SecretSelectInputElement(), null, null, null)).isInstanceOf(ResponseException.class);
        verifyNoInteractions(secrets);
    }

    @Test
    void paginatesOptionsAndRejectsUnboundedRequests() throws Exception {
        var field = new SelectInputElement().setOptions(java.util.stream.IntStream.range(0, 55).mapToObj(i -> SelectInputElementOption.of("v" + i, "Option " + i)).toList());
        var result = mapper.valueToTree(service.options(scope, node, field, null, null, null));
        assertThat(result.path("items")).hasSize(20);
        assertThat(result.path("nextOffset").asInt()).isEqualTo(20);
        assertThatThrownBy(() -> service.options(scope, node, field, null, 0, 51)).isInstanceOf(IllegalArgumentException.class);
    }
}
