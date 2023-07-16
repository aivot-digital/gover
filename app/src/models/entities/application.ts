import { type RootElement } from '../elements/root-element';
import { type ListApplication } from './list-application';


export interface Application extends ListApplication {
    root: RootElement;

    destination?: number | null;

    legalSupportDepartment?: number | null;
    technicalSupportDepartment?: number | null;
    imprintDepartment?: number | null;
    privacyDepartment?: number | null;
    accessibilityDepartment?: number | null;

    customerAccessHours?: number | null;
    submissionDeletionWeeks?: number | null;

    created: string;
    updated: string;
}

export function isApplication(obj: any): obj is Application {
    return obj != null && 'slug' in obj;
}
