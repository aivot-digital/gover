package de.aivot.GoverBackend.openApi;

public class OpenApiConstants {
    public static class Tags {
        public static final String FormsName = "Forms";
        public static final String FormsDescription = "APIs for managing forms";

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

        public static final String TeamUserRoleAssignmentsWithDetails = "Team User Role Assignments";
        public static final String TeamUserRoleAssignmentsWithDetailsDescription =
                "User roles are assigned to users within the context of a team membership. " +
                "This allows for granular control over user permissions and access rights specific to each team.";
    }
}
