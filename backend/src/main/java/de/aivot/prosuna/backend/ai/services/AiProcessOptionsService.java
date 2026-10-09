package de.aivot.prosuna.backend.ai.services;

import de.aivot.prosuna.backend.ai.models.AiProcessChatContext;
import de.aivot.prosuna.backend.asset.services.VStorageIndexItemWithAssetService;
import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.prosuna.backend.asset.permissions.AssetPermissionProvider;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import de.aivot.prosuna.backend.dataObject.services.DataObjectSchemaService;
import de.aivot.prosuna.backend.dataObject.services.DataObjectItemService;
import de.aivot.prosuna.backend.dataObject.filters.DataObjectSchemaFilter;
import de.aivot.prosuna.backend.dataObject.filters.DataObjectItemFilter;
import de.aivot.prosuna.backend.dataObject.permissions.DataObjectPermissionProvider;
import de.aivot.prosuna.backend.department.filters.VDepartmentShadowedFilter;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.services.CodeListElementOptionsService;
import de.aivot.prosuna.backend.identity.filters.IdentityProviderFilter;
import de.aivot.prosuna.backend.identity.permissions.IdentityProviderPermissionProvider;
import de.aivot.prosuna.backend.identity.services.IdentityProviderService;
import de.aivot.prosuna.backend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.dtos.NoCodeOperatorDetailsDTO;
import de.aivot.prosuna.backend.nocode.providers.NoCodeOperatorsProvider;
import de.aivot.prosuna.backend.payment.filters.PaymentProviderFilter;
import de.aivot.prosuna.backend.payment.permissions.PaymentProviderPermissionProvider;
import de.aivot.prosuna.backend.payment.services.PaymentProviderService;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.PotentialProcessInstanceAccessService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.secrets.filters.SecretFilter;
import de.aivot.prosuna.backend.secrets.permissions.SecretPermissionProvider;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.filters.StorageProviderFilter;
import de.aivot.prosuna.backend.storage.permissions.StoragePermissionProvider;
import de.aivot.prosuna.backend.storage.services.StorageProviderService;
import de.aivot.prosuna.backend.teams.filters.TeamFilter;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.teams.services.TeamService;
import de.aivot.prosuna.backend.user.filters.UserFilter;
import de.aivot.prosuna.backend.user.permissions.UserPermissionProvider;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

@Service
public class AiProcessOptionsService {
    private final VStorageIndexItemWithAssetService assets;
    private final DataObjectSchemaService dataSchemas;
    private final DataObjectItemService dataItems;
    private final PermissionService permissions;
    private final PotentialProcessInstanceAccessService access;
    private final TeamService teams;
    private final VDepartmentShadowedService departments;
    private final UserService users;
    private final SecretService secrets;
    private final StorageProviderService storage;
    private final IdentityProviderService identities;
    private final PaymentProviderService payments;
    private final ProcessNodeService nodes;
    private final CodeListElementOptionsService codeLists;
    private final List<NoCodeOperatorsProvider> noCode;
    private final List<JavascriptFunctionProvider> javascript;
    private final JsonMapper mapper;

    public AiProcessOptionsService(@Nonnull VStorageIndexItemWithAssetService assets, @Nonnull DataObjectSchemaService dataSchemas, @Nonnull DataObjectItemService dataItems, @Nonnull PermissionService permissions, @Nonnull PotentialProcessInstanceAccessService access,
                                   @Nonnull TeamService teams, @Nonnull VDepartmentShadowedService departments, @Nonnull UserService users,
                                   @Nonnull SecretService secrets, @Nonnull StorageProviderService storage, @Nonnull IdentityProviderService identities,
                                   @Nonnull PaymentProviderService payments, @Nonnull ProcessNodeService nodes, @Nonnull CodeListElementOptionsService codeLists,
                                   @Nonnull List<NoCodeOperatorsProvider> noCode, @Nonnull List<JavascriptFunctionProvider> javascript, @Nonnull JsonMapper mapper) {
        this.assets = assets;
        this.dataSchemas = dataSchemas;
        this.dataItems = dataItems;
        this.permissions = permissions;
        this.access = access;
        this.teams = teams;
        this.departments = departments;
        this.users = users;
        this.secrets = secrets;
        this.storage = storage;
        this.identities = identities;
        this.payments = payments;
        this.nodes = nodes;
        this.codeLists = codeLists;
        this.noCode = noCode;
        this.javascript = javascript;
        this.mapper = mapper;
    }

    @Nonnull
    public Object options(@Nonnull AiProcessChatContext scope, @Nonnull ProcessNodeEntity node, @Nonnull BaseInputElement<?> input,
                          @Nullable String query, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        List<?> values;
        var resolved = codeLists.resolve(input);
        if (resolved instanceof SelectInputElement select) values = Objects.requireNonNullElse(select.getOptions(), List.of());
        else if (resolved instanceof RadioInputElement radio) values = Objects.requireNonNullElse(radio.getOptions(), List.of());
        else if (resolved instanceof MultiCheckboxInputElement multi) values = Objects.requireNonNullElse(multi.getOptions(), List.of());
        else if (resolved instanceof ChipInputElement chip) values = Objects.requireNonNullElse(chip.getSuggestions(), List.of());
        else if (input instanceof DomainAndUserSelectInputElement domain) values = assignees(scope, domain.getAllowedTypes(), domain.getProcessAccessConstraint());
        else if (input instanceof AssignmentContextInputElement assignment) values = assignees(scope, assignment.getAllowedTypes(), assignment.getProcessAccessConstraint());
        else if (input instanceof SecretSelectInputElement) {
            permissions.requireSystemPermission(scope.userId(), SecretPermissionProvider.SECRET_READ);
            values = secrets.list(Pageable.unpaged(), SecretFilter.create().setName(query)).stream()
                    .map(secret -> Map.of("value", secret.getKey().toString(), "label", secret.getName())).toList();
        } else if (input instanceof StoragePathSelectorInputElement selector) {
            permissions.requireSystemPermission(scope.userId(), StoragePermissionProvider.STORAGE_PROVIDER_READ);
            values = storage.list(Pageable.unpaged(), StorageProviderFilter.create().setName(query)).stream()
                    .filter(provider -> selector.getAllowedStorageProviderTypes() == null || selector.getAllowedStorageProviderTypes().isEmpty()
                            || selector.getAllowedStorageProviderTypes().contains(provider.getType()))
                    .map(provider -> Map.of("storageProviderId", provider.getId(), "label", provider.getName(), "type", provider.getType())).toList();
        } else if (input instanceof IdentityConfigElement) {
            permissions.requireSystemPermission(scope.userId(), IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ);
            values = identities.list(Pageable.unpaged(), IdentityProviderFilter.create().setName(query)).stream()
                    .map(provider -> Map.of("key", provider.getKey().toString(), "label", provider.getName())).toList();
        } else if (input instanceof PaymentConfigElement) {
            permissions.requireSystemPermission(scope.userId(), PaymentProviderPermissionProvider.PAYMENT_PROVIDER_READ);
            values = payments.list(Pageable.unpaged(), PaymentProviderFilter.create().setName(query)).stream()
                    .map(provider -> Map.of("key", provider.getKey().toString(), "label", provider.getName())).toList();
        } else if (input instanceof AssetSelectInputElement selector) {
            permissions.requireSystemPermission(scope.userId(), AssetPermissionProvider.ASSET_READ);
            var providerIds = storage.listAllByType(StorageProviderType.Assets).stream().map(provider -> provider.getId()).toList();
            if (providerIds.isEmpty()) return AiToolResults.page(List.of(), offset, limit);
            var spec = SpecificationBuilder.create(VStorageIndexItemWithAssetEntity.class)
                    .withInList("storageProviderId", providerIds).withEquals("directory", false)
                    .withEquals("missing", false).withNotNull("assetKey").withContains("filename", query).build();
            values = assets.list(Pageable.unpaged(), () -> spec).stream()
                    .filter(asset -> selector.getAssetVisibility() == AssetVisibility.All
                            || selector.getAssetVisibility() == AssetVisibility.Public && Boolean.FALSE.equals(asset.getAssetIsPrivate())
                            || selector.getAssetVisibility() == AssetVisibility.Private && !Boolean.FALSE.equals(asset.getAssetIsPrivate()))
                    .filter(asset -> selector.getAllowedMimeTypes() == null || selector.getAllowedMimeTypes().isEmpty()
                            || selector.getAllowedMimeTypes().stream().anyMatch(type -> type.equalsIgnoreCase(asset.getMimeType())
                                || type.endsWith("/*") && asset.getMimeType().toLowerCase(Locale.ROOT).startsWith(type.substring(0, type.length() - 1).toLowerCase(Locale.ROOT))))
                    .map(asset -> Map.of("value", asset.getAssetKey().toString(), "label", asset.getFilename(), "mimeType", asset.getMimeType())).toList();
        } else if (input instanceof DataModelSelectInputElement) {
            permissions.requireSystemPermission(scope.userId(), DataObjectPermissionProvider.OBJECT_SCHEMA_READ);
            values = dataSchemas.list(Pageable.unpaged(), new DataObjectSchemaFilter().setName(query)).stream()
                    .map(schema -> Map.of("value", schema.getKey(), "label", schema.getName())).toList();
        } else if (input instanceof DataObjectSelectInputElement selector) {
            permissions.requireSystemPermission(scope.userId(), DataObjectPermissionProvider.OBJECT_ITEM_READ);
            if (selector.getDataModelKey() == null || selector.getDataModelKey().isBlank()) throw ResponseException.badRequest("Für dieses Feld ist kein Datenmodell festgelegt.");
            dataSchemas.retrieve(selector.getDataModelKey()).orElseThrow(ResponseException::notFound);
            values = dataItems.list(Pageable.unpaged(), DataObjectItemFilter.create().setSchemaKey(selector.getDataModelKey())).stream()
                    .map(item -> Map.of("value", item.getId(), "label", selector.getDataLabelAttributeKey() == null ? item.getId()
                            : String.valueOf(item.getData().getOrDefault(selector.getDataLabelAttributeKey(), item.getId())))).toList();
        } else if (input instanceof ProcessIdentityIdInputElement) {
            values = nodes.getIncomingProcessNodeDefinitionMetadata(node, users.retrieve(scope.userId()).orElseThrow(ResponseException::unauthorized)).forwardedIdentities().stream()
                    .map(identity -> Map.of("value", identity.identityId(), "label", Objects.requireNonNullElse(identity.label(), identity.identityId()))).distinct().toList();
        } else if (input instanceof ProcessInstanceAttachmentSetSelectElement) {
            values = nodes.getIncomingProcessNodeDefinitionMetadata(node, users.retrieve(scope.userId()).orElseThrow(ResponseException::unauthorized))
                    .forwardedAttachmentSets().stream().map(set -> Map.of("value", set.dataKey(), "label", set.label())).distinct().toList();
        } else {
            return Map.of("supported", false, "message", "Für diesen Feldtyp ist keine Optionssuche verfügbar. Nutzen Sie das Wertschema und die Feldhinweise.");
        }
        return AiToolResults.page(values.stream().filter(value -> AiToolResults.matches(query, mapper.writeValueAsString(value))).toList(), offset, limit);
    }

    private List<?> assignees(AiProcessChatContext scope, List<String> allowedTypes, DomainAndUserSelectProcessAccessConstraint constraint) throws ResponseException {
        if (constraint != null && constraint.getProcessId() != null && constraint.getProcessVersion() != null) {
            if (!Objects.equals(scope.processId(), constraint.getProcessId()) || !Objects.equals(scope.processVersion(), constraint.getProcessVersion())) throw ResponseException.badRequest("Die Feldauswahl verweist auf einen anderen Prozesskontext.");
            permissions.requireProcessPermission(scope.userId(), scope.processId(), ProcessPermissionProvider.PROCESS_DEFINITION_UPDATE);
            return access.listSelectableItems(scope.processId(), scope.processVersion(), constraint.getRequiredPermissions()).stream()
                    .filter(item -> enabled(allowedTypes, item.type()))
                    .map(item -> Map.of("value", Map.of("type", item.type(), "id", item.id()), "label", item.label())).toList();
        }
        var result = new ArrayList<Map<String, Object>>();
        if (enabled(allowedTypes, "team")) {
            var filter = TeamFilter.create();
            boolean global = permissions.hasSystemPermission(scope.userId(), TeamPermissionProvider.TEAM_READ);
            var ids = global ? List.<Integer>of() : permissions.getTeamsWithPermission(scope.userId(), TeamPermissionProvider.TEAM_READ);
            if (global || !ids.isEmpty()) {
                if (!global) filter.setIds(ids);
                teams.list(Pageable.unpaged(), filter).forEach(team -> result.add(option("team", team.getId().toString(), team.getName())));
            }
        }
        if (enabled(allowedTypes, "orgUnit")) {
            var filter = VDepartmentShadowedFilter.create();
            boolean global = permissions.hasSystemPermission(scope.userId(), DepartmentPermissionProvider.DEPARTMENT_READ);
            var ids = global ? List.<Integer>of() : permissions.getDepartmentsWithPermission(scope.userId(), DepartmentPermissionProvider.DEPARTMENT_READ);
            if (global || !ids.isEmpty()) {
                if (!global) filter.setIds(ids);
                departments.list(Pageable.unpaged(), filter).forEach(department -> result.add(option("orgUnit", department.getId().toString(), department.getName())));
            }
        }
        if (enabled(allowedTypes, "user") && permissions.hasSystemPermission(scope.userId(), UserPermissionProvider.USER_READ)) {
            users.list(Pageable.unpaged(), UserFilter.create().setDeletedInIdp(false).setDisabledInIdp(false))
                    .forEach(user -> result.add(option("user", user.getId(), user.getFullName())));
        }
        return result;
    }

    private boolean enabled(List<String> allowed, String type) { return allowed == null || allowed.isEmpty() || allowed.contains(type); }
    private Map<String, Object> option(String type, String id, String label) { return Map.of("value", Map.of("type", type, "id", id), "label", label); }

    @Nonnull
    public Object help(@Nonnull String topic, @Nullable String key, @Nullable Integer offset, @Nullable Integer limit) throws ResponseException {
        return switch (topic) {
            case "modes" -> Map.of(
                    "Literal", Map.of("mode", "Literal", "value", "Rohwert gemäß literalValueSchema; null ist erlaubt"),
                    "Variable", Map.of("mode", "Variable", "value", Map.of("source", "ProcessData", "path", "Pfad aus liste-knotenvariablen")),
                    "NoCode", Map.of("mode", "NoCode", "value", Map.of("type", "NoCodeExpression", "operatorIdentifier", "Kennung aus nocode-operators", "operands", List.of())),
                    "LowCode", Map.of("mode", "LowCode", "value", "JavaScript-Funktionskörper mit return"),
                    "note", "Nur erlaubte Feldmodi verwenden. Konfigurationswerte werden über valuePath gesetzt; removePaths entfernt Einträge ausdrücklich. Dynamische Werte werden beim Bearbeiten nicht mit Vorgangsdaten ausgeführt.");
            case "nocode-operators" -> AiToolResults.page(noCode.stream().flatMap(NoCodeOperatorDetailsDTO::fromSPI)
                    .filter(operator -> AiToolResults.matches(key, operator.identifier() + " " + operator.label()))
                    .sorted(Comparator.comparing(NoCodeOperatorDetailsDTO::identifier))
                    .map(operator -> Map.of("key", operator.identifier(), "label", operator.label(), "provider", operator.packageName())).toList(), offset, limit);
            case "nocode-operator" -> noCode.stream().flatMap(NoCodeOperatorDetailsDTO::fromSPI)
                    .filter(operator -> operator.identifier().equals(key)).findFirst().orElseThrow(ResponseException::notFound);
            case "nocode-operands" -> Map.of("expression", Map.of("type", "NoCodeExpression", "operatorIdentifier", "Operator-Kennung", "operands", List.of()),
                    "literal", Map.of("type", "NoCodeStaticValue", "value", "Beliebiger JSON-Wert"),
                    "processData", Map.of("type", "NoCodeProcessDataReference", "path", "Pfad aus liste-knotenvariablen"),
                    "element", Map.of("type", "NoCodeReference", "elementId", "Feld-ID aus dem Layout"),
                    "instanceData", Map.of("type", "NoCodeInstanceDataReference", "path", "Pfad aus liste-knotenvariablen"),
                    "nodeData", Map.of("type", "NoCodeNodeDataReference", "nodeDataKey", "Knoten-Datenschlüssel", "path", "Pfad aus liste-knotenvariablen"));
            case "javascript" -> key == null
                    ? AiToolResults.page(javascript.stream().map(provider -> Map.of("key", provider.getObjectName(), "name", provider.getName())).toList(), offset, limit)
                    : AiToolResults.page(Arrays.asList(javascript.stream().filter(provider -> provider.getObjectName().equals(key))
                        .findFirst().orElseThrow(ResponseException::notFound).getMethodTypeDefinitions()), offset, limit);
            default -> throw ResponseException.badRequest("Unbekanntes Hilfethema. Verfügbar: modes, nocode-operators, nocode-operator, nocode-operands, javascript.");
        };
    }
}
