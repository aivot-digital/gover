export interface TeamMembershipResponseDTO {
    id: number;
    teamId: number;
    userId: string;
    created: string; // ISO date string
    updated: string; // ISO date string
}
