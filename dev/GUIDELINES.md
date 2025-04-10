# Modules

Each module is a package in the GoverBackend.
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
These properties are fetched from the `application.properties` file.
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
Services should check the permissions of the user before executing any action.
Services should log to the audit log before executing any action.

## package-info.java

The package-info.java file should contain the module name and description.

# Audit Logs

All actions should be logged in the audit log.