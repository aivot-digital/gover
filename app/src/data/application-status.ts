export enum ApplicationStatus {
    Drafted = 0,
    InReview = 1,
    Published = 2,
    Revoked = 3,
}

export const ApplicationStatusNames: Record<ApplicationStatus, string> = {
    [ApplicationStatus.Drafted]: 'In Bearbeitung',
    [ApplicationStatus.InReview]: 'Wartet auf Freigabe',
    [ApplicationStatus.Published]: 'Veröffentlicht',
    [ApplicationStatus.Revoked]: 'Zurückgezogen',
};
