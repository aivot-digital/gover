export interface VDepartmentShadowedEntity {
    id: number;
    name: string;
    postalAddress?: string | null;
    imprint?: string | null;
    commonPrivacy?: string | null;
    commonAccessibility?: string | null;
    technicalSupportEmail?: string | null;
    specialSupportEmail?: string | null;
    created: string;
    updated: string;
    themeId?: number | null;
    technicalSupportPhone?: string | null;
    technicalSupportInfo?: string | null;
    specialSupportPhone?: string | null;
    specialSupportInfo?: string | null;
    defaultMailSignature?: string | null;
    depth: number;
    parentDepartmentId?: number | null;
    parentNames?: string[] | null;
    parentIds?: number[] | null;
}

export interface VDepartmentShadowedEntityWithChildren extends VDepartmentShadowedEntity {
    children: VDepartmentShadowedEntityWithChildren[];
}

export interface PublicDepartmentResponseDTO {
    id: number;
    postalAddress?: string | null;
    imprint?: string | null;
    commonPrivacy?: string | null;
    commonAccessibility?: string | null;
    technicalSupportEmail?: string | null;
    technicalSupportPhone?: string | null;
    technicalSupportInfo?: string | null;
    specialSupportEmail?: string | null;
    specialSupportPhone?: string | null;
    specialSupportInfo?: string | null;
    defaultMailSignature?: string | null;
    themeId?: number | null;
    parentDepartmentId?: number | null;
    parentIds?: number[] | null;
}
