package de.aivot.GoverBackend.openApi;

public class OpenApiConstants {
    public static class Tags {
        public static final String AssetsName = "Assets";
        public static final String AssetsDescription = "Assets are files uploaded to the system, such as images or documents. " +
                "They can be associated with various entities within the application and should be used if you need to provides files to citizens publicly.";

        public static final String PermissionsName = "Permissions";
        public static final String PermissionsDescription = "Permissions define the access rights for users and roles within the system. " +
                "They determine what actions can be performed on different entities and resources, ensuring proper access control and security.";

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
