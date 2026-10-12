# Modules

Each module is a package in the Prosuna backend.
A module encapsulates all controllers, models, services, repositories etc. that are related to a specific purpose of the module.
Each module should contain a `package-info.java` file which contains the module name and description.

```bash
./example
├── configs
│         ├── ExampleSystemConfig.java
│         └── ExampleUserConfig.java
├── controllers
│         └── ExampleController.java
├── dtos
│         ├── ExampleEntityRequestDTO.java
│         └── ExampleEntityResponseDTO.java
├── entites
│         └── ExampleEntity.java
├── enums
│         └── ExampleEnum.java
├── exceptions
│         └── ExampleException.java
├── javascript
│         └── ExampleJavascriptPlugin.java
├── models
│         └── ExampleModel.java
├── properties
│         └── ExampleProperties.java
├── repositories
│         └── ExampleRepository.java
├── services
│         ├── ExampleService.java
│         └── ExampleStartupService.java
└── package-info.java
```

## Configs

Configs contain the config definitions for this module.
Configs can be system-wide or user specific.
Configs should be used to define the configuration of the module.

## Controllers

Controllers should only work with DTO objects which are an abstraction of the entities they are controlling.
There should always be a request and response DTO for each entity.
Controllers should not contain any business logic and should only be used to handle requests and responses.
Upon receiving a request, the controller should validate the request and pass it to the service as the entity the service manages.

## DTOs

There are two types of DTOs: Request and Response DTOs.
Request DTOs should always be used in the controllers to receive data from the client.
Response DTOs should always be used in the controllers to send data to the client.
DTOs which represent Entities should contain static methods to transform them into the entity or be constructed from the entity.
DTOs should not contain any business logic.
Request DTO **must** include validation annotations from jakarta.
Make sure to include methods to create a response DTO from an entity `fromEntity()` and to create an entity from a request DTO `toEntity()`.

## Entities

Entities are the representation of database tables.
They should be used in the repositories to interact with the database.
Entities should not contain any business logic.

## Enums

Enums contain specific enumerations that are used in the module.

## Exceptions

Exceptions contain module specific exceptions that can be thrown by the module.
If an exception is thrown by the module, it should be caught and handled by the module.
All exceptions should result in a reponse to the client with one of the core exceptions.

## Javascript

Javascript contains all javascript plugins that are used in the module.

## Models

Models are related to business logic and serve as representations of the data that is being used in the module.
Models should never be returned to the user directly.

## Properties

Properties contain module specific properties that are used in the module.
These properties are fetched from the `application.yml` file.
Properties should only be used in the module they are defined in.

## Repositories

Repositories are used to interact with the database.
Repositories should only be used to interact with the database and should not contain any business logic.
Repositories should only be used in the services.
Repositories should only be used in the module they are defined in.

## Services

Services contain the business logic of the module.
Services can and should be used to interact with the module from other modules.
Services always operator with entities or models but never with DTOs.
Prefer permission checks in services before protected operations, so all entry paths enforce the same authorization rules.
Prefer audit logging in services as part of the auditable operation; see Audit Logs below.
Permission checks or audit logging in controllers are exceptions when necessary or appropriate for the operation. Keep service entry paths protected and avoid duplicate audit events.
Apply this policy to the requested change without relocating unrelated existing checks or logging. See [AGENTS.md](../AGENTS.md) for permission scopes and verification rules.

## package-info.java

The package-info.java file should contain the module name and description.

# Audit Logs

Preserve existing auditable actions, including staff-side mutations, and add the relevant `ScopedAuditService` events when extending them.
Record the actual outcome: do not log an operation as successful before it succeeds. Failure or denied-access events, where applicable, are distinct from success events.
Prefer services for these events, with justified controller exceptions. Cover changed audit behavior with focused tests.

# Process Node Ports

Implement `ProcessNodeDefinition.getPorts(@Nonnull NodeConfig configuration)` to return the outgoing connections available for a node's authored configuration. The former parameterless method is no longer supported; rebuild external plugins against the new interface and deploy matching backend and frontend versions together.

Use deterministic order and stable, unique keys of at most 32 characters. For configurable choices, store an explicit stable key in the configuration instead of deriving it from a label or row index. Fields determining the port set must use literal-only inputs; runtime variables, templates and expressions may decide which port is selected during execution, but must not change the available ports. Handle incomplete authoring configuration and null fields gracefully and report required configuration through the existing validation contract.

Implement `getDeadlines(configuration)` to offer an ordered list of `ProcessNodeDeadlineDefinition` values (empty by default). Each definition has a stable key (at most 32 characters), label, description and a provider-defined expiry `ProcessNodePort`. Deadline keys and all regular/expiry port keys must be unique. Fields determining the deadline list must also be literal-only and tolerate incomplete authoring values. Return ports without `deadlineKey`; the platform sets this read-only field on configured hard-deadline ports.

A node stores `deadlines: { [key]: { days, strict } }`. Durations are optional whole days from 1 to 3652; an absent duration means no deadline and `strict` defaults to false. The platform adds one expiry port per offered deadline with a duration and `strict: true`. Hidden settings remain stored but produce no ports. Every available port must be connected before publication. Switching to soft, clearing the duration or hiding a phase preserves existing connections as invalid edges until corrected. When an active hard deadline expires, the platform ends the task with the status `DeadlineExceeded`, stores the expiry details under the reserved element data key `_deadlineExceeded` and continues the process through the expiry port; later staff and customer changes to the task are rejected.

An execution result selects a phase through `setNewDeadlineKey(key)`. The handler validates that the phase is offered before communication or assignment side effects, calculates its date from phase entry using local business time and persists both `deadline` and `deadlineKey` on the task. Selecting an unconfigured phase clears both fields. A null result key preserves the current deadline, and selecting the already active key does not restart it. Autosave and polling results must leave the selection unset. Changes and removals produce a history-relevant event, and assignment emails see the updated date. Task completion retains the last deadline. An expiry port can only complete a task whose active deadline key matches that port.

Form and payment requests offer a message-dispatch deadline for manual preparation and a customer deadline starting with dispatch. Manual message/email nodes, manual tasks, approvals, data changes and manual instance assignments offer their corresponding staff phase. The form trigger offers a payment phase when a payment provider is configured and activates it only when payment is requested.

This replaces the former single node deadline and absolute execution-result override without compatibility conversion. Recreate development databases after pulling the consolidated development migrations; rebuild external plugins and update process exports to the new schema.

Provider metadata no longer contains `ports`. Node list, detail, create, update and import responses contain computed `ports`, `portResolutionProblems` and `deadlineDefinitions`. These fields are read-only and are not persisted or exported. Use `POST /api/process-nodes/resolve-ports/` with `{node, forImport, useInitialConfiguration}` to inspect a prospective node without saving it; process read permission is required. Set `useInitialConfiguration` for new nodes and `forImport` for imported nodes. Replacements resolve their actual replacement configuration. The response contains `{ports, problems, deadlineDefinitions}`.

The editor previews available deadlines from unsaved configuration and updates graph ports after saving. Edges referencing a removed port remain stored, are shown as unavailable connections, and prevent publication until corrected. A failed port resolution is an authoring problem, not a valid empty set. All backend connection checks and execution transitions use the shared authoring port resolution.
