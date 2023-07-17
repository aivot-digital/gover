import {type ApplicationStatus} from '../../data/application-status/application-status';

export interface ListApplication {
    id: number;
    slug: string;
    version: string;
    title: string;
    headline: string;
    theme?: number | null;
    status: ApplicationStatus;
    developingDepartment: number;
    managingDepartment?: number | null;
    responsibleDepartment?: number | null;
    created: string;
    updated: string;
    totalSubmissions: number;
    inProgressSubmissions: number;
    openSubmissions: number;
}
