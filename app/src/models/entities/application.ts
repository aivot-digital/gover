import { type ApplicationStatus } from '../../data/application-status/application-status';
import { type RootElement } from '../elements/root-element';


export interface Application {
    id: number;
    slug: string;
    version: string;
    title: string;
    status: ApplicationStatus;
    root: RootElement;

    destination?: number | null;

    developingDepartment: number;

    legalSupportDepartment?: number | null;
    technicalSupportDepartment?: number | null;
    imprintDepartment?: number | null;
    privacyDepartment?: number | null;
    accessibilityDepartment?: number | null;
    managingDepartment?: number | null;
    responsibleDepartment?: number | null;

    customerAccessHours?: number | null;
    submissionDeletionWeeks?: number | null;

    theme?: number | null;

    created: string;
    updated: string;
}
