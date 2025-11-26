export interface DepartmentResponseDTO {
    id: number;
    parentOrgUnitId: number | null | undefined;
    depth: number;
    name: string;
    address: string | null | undefined;
    imprint: string | null | undefined;
    commonPrivacy: string | null | undefined;
    commonAccessibility: string | null | undefined;
    technicalSupportAddress: string | null | undefined;
    technicalSupportPhone: string | null | undefined;
    technicalSupportInfo: string | null | undefined;
    specialSupportAddress: string | null | undefined;
    specialSupportPhone: string | null | undefined;
    specialSupportInfo: string | null | undefined;
    additionalInfo: string | null | undefined;
    departmentMail: string | null | undefined;
    themeId: number | null | undefined;
    created: string;
    updated: string;
}

export interface VOrganizationalUnitShadowedResponseDTO extends DepartmentResponseDTO {
    parentNames: string[] | null | undefined;
    parentIds: number[] | null | undefined;
}