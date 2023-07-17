import {ApplicationStatus} from './application-status';

export const ApplicationStatusNames: {
    [key in ApplicationStatus]: string;
} = {
    [ApplicationStatus.Drafted]: 'In Bearbeitung',
    [ApplicationStatus.InReview]: 'Wartet auf Freigabe',
    [ApplicationStatus.Published]: 'Veröffentlicht',
    [ApplicationStatus.Revoked]: 'Zurückgezogen',
};
