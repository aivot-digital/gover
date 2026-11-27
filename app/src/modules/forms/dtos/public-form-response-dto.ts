import type {ProcessVersionEntity} from '../../process/entities/process-version-entity';

// The public form endpoints never send the node configuration or internal process data to customers. These types only
// contain what the customer form and the public form index use.

export interface PublicFormNodeDTO {
    id: number;
    formSlug: string | null;
}

export interface PublicFormProcessDTO {
    slug: string;
}

export type PublicFormVersionDTO = Pick<
    ProcessVersionEntity,
    | 'processVersion'
    | 'publicTitle'
    | 'updated'
    | 'legalSupportDepartmentId'
    | 'technicalSupportDepartmentId'
    | 'imprintDepartmentId'
    | 'privacyDepartmentId'
    | 'accessibilityDepartmentId'
    | 'processSpecificPrivacyStatement'
    | 'processSpecificAccessibilityStatement'
>;

export interface PublicFormListItemDTO {
    processSlug: string;
    formSlug: string | null;
    processVersion: number;
    formTitle: string;
    updated: string;
}
