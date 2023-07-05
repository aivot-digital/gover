import {ApplicationStatus} from "../../data/application-status/application-status";

export interface ListApplication {
    id: number;
    slug: string;
    version: string;
    title: string;
    headline: string;
    theme: string;
    status: ApplicationStatus;
    developingDepartment: number;
    managingDepartment: number;
    responsibleDepartment: number;
    created: string;
    updated: string;
}
