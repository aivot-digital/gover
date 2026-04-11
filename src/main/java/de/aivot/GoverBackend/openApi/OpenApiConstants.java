package de.aivot.GoverBackend.openApi;

public class OpenApiConstants {
    public static class Tags {
        public static final String AuditLogsName = "Audit Logs";
        public static final String AuditLogsDescription = "Audit logs record relevant actions performed in the system, including metadata such as actor, component, action type, and optional data diffs.";

        public static final String AssetsName = "Assets";
        public static final String AssetsDescription = "Assets are files uploaded to the system, such as images or documents. " +
                "They can be associated with various entities within the application and should be used if you need to provides files to citizens publicly.";

        public static final String CaptchaName = "Captcha";
        public static final String CaptchaDescription = "Captcha challenges are used to prevent automated abuse of the system. " +
                "They require clients to solve a proof-of-work challenge before performing certain actions. " +
                "This endpoint is required for certain, unauthenticated api actions.";

        public static final String SystemConfigsName = "System Configs";
        public static final String SystemConfigDescription = "System configurations are key-value pairs that define various settings and parameters of the application. " +
                "These configurations can be used to customize the behavior of the system and should be used if you need to provide configuration values to citizens publicly.";

        public static final String UserConfigsName = "User Configurations";
        public static final String UserConfigDescription = "User configurations are key-value pairs that define various settings and preferences for individual users. " +
                "These configurations can be used to customize the behavior of the system for each user.";

        public static final String DataObjectName = "Data Objects";
        public static final String DataObjectsDescription = "Data Objects are used to define and manage data structures within the system. " +
                "They can be used to define complex data structures, such as complex data models, or to define simple data structures, such as simple data types. " +
                "Data Objects are separated into Schemas and Items. " +
                "A Data Object Schema defines the structure of a data object, including its properties and their types. " +
                "A Data Object Item is an instance of a data object schema, containing specific values for the properties defined in the schema.";

        public static final String DataObjectItemsName = DataObjectName;
        public static final String DataObjectItemsDescription = DataObjectsDescription;

        public static final String DataObjectSchemasName = DataObjectItemsName;
        public static final String DataObjectSchemasDescription = DataObjectsDescription;

        public static final String DepartmentsName = "Departments";
        public static final String DepartmentsDescription = "Departments are used to group users together for organizational purposes. " +
                "Users can be assigned to multiple departments via memberships, and can have different roles within the department. " +
                "Departments are hierarchical, meaning that they can have parent-child relationships, allowing for a structured organization within the system. " +
                "Properties of departments are inherited by their child departments. " +
                "Memberships in the departments, as well as permissions are not inherited from a parent department.";

        public static final String DepartmentMembershipsName = DepartmentsName;
        public static final String DepartmentMembershipsDescription = DepartmentsDescription;

        public static final String DestinationsName = "Destinations";
        public static final String DestinationsDescription = "Destinations are used to define and manage destinations within the system. " +
                "They can be used to define different types of destinations, such as email addresses, URLs, or other destinations. " +
                "When a form is submitted, the data can be sent to the destination specified by the form.";

        public static final String ElementsName = "Elements";
        public static final String ElementsDescription = "Elements are building blocks for creating complex interfaces in the application. " +
                "They can represent various UI components, data structures, or functional units that can be combined to form complete views or functionalities.";






        public static final String PermissionsName = "Permissions";
        public static final String PermissionsDescription = "Permissions define the access rights for users and roles within the system. " +
                "They determine what actions can be performed on different entities and resources, ensuring proper access control and security.";

        public static final String PluginsName = "Plugins";
        public static final String PluginsDescription = "Plugins extend the application platform with additional features and functionalities. " +
                "They can provide new capabilities, integrations, or customizations to enhance the user experience and meet specific requirements. " +
                "Plugins can be developed and installed to tailor the system to the needs of different organizations and use cases.";

        public static final String ProcessAccessControlsName = "Process Access Controls";
        public static final String ProcessAccessControlsDescription =
                "Process Access Controls define the permissions for teams and departments to access and interact with specific processes. " +
                        "They specify which actions can be performed on a process by members of a team or department, ensuring proper access management and security within the workflow system.";

        public static final String ProcessTestClaimsName = "Process Test Claims";
        public static final String ProcessTestClaimsDescription =
                "Process Test Claims are used to allow process instantiation without the need to publish the process, as well as testing already published processes. " +
                        "They allow developers and testers to validate the behavior and functionality of processes without affecting real data or users. " +
                        "A process needs a test claim in order to be tested.";

        public static final String ProcessesDefinitionsName = "Processes";
        public static final String ProcessesDefinitionsDescription =
                "Processes are used to define and manage workflows within the system. " +
                        "They are versioned and consist of a series of steps and transitions that dictate how tasks are assigned and completed. " +
                        "Processes can be initiated by users or system events, and their progress can be tracked and monitored.";

        public static final String TeamsName = "Teams";
        public static final String TeamsDescription =
                "Teams are used to group users together for cross-departmental collaboration. " +
                        "Users can be assigned to multiple teams, and can have different roles within the team. " +
                        "Teams can be associated with various entities in the system to manage permissions and access control.";

        public static final String TeamMembershipsName = "Team Memberships";
        public static final String TeamMembershipsDescription =
                "Team memberships are used to assign users to teams. " +
                        "The permissions of a user inside a team are determined by the team roles assigned to the membership.";

        public static final String SystemRolesName = "System Roles";
        public static final String SystemRolesDescription = "System roles define the permissions and access levels for users across the entire system.";

        public static final String StorageProvidersName = "Storage Providers";
        public static final String StorageProvidersDescription = "Storage providers are services that handle the storage and retrieval of files and data within the system. " +
                "They can be configured to use different backends, such as local file systems or cloud storage solutions.";

        public static final String UserDeputiesName = "User Deputies";
        public static final String UserDeputiesDescription = "User deputies allow users to delegate their responsibilities to other users for a specified period of time. " +
                "This is useful for managing workloads and ensuring continuity in case of absences.";
    }
}
