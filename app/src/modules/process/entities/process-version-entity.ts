import {ProcessStatus} from '../enums/process-status';

export interface ProcessVersionEntity {
    processId: number;
    processVersion: number;
    status: ProcessStatus;
    publicTitle: string;
    caseNumberTemplate: string | null;
    notes: string | null;
    themeId: number | null;
    legalSupportDepartmentId: number | null;
    technicalSupportDepartmentId: number | null;
    imprintDepartmentId: number | null;
    privacyDepartmentId: number | null;
    accessibilityDepartmentId: number | null;
    processSpecificPrivacyStatement: string | null;
    processSpecificAccessibilityStatement: string | null;
    crated: string;
    updated: string;
    published: string | null;
    revoked: string | null;
}
