export interface DepartmentResponseDTO {
    id: number;
    name: string;
    address: string | null | undefined;
    imprint: string | null | undefined;
    privacy: string | null | undefined;
    accessibility: string | null | undefined;
    technicalSupportAddress: string | null | undefined;
    specialSupportAddress: string | null | undefined;
    departmentMail: string | null | undefined;
    themeId: number | null | undefined;
    contactLegal: string | null | undefined;
    contactTechnical: string | null | undefined;
    additionalInfo: string | null | undefined;
    depth: number;
    parentOrgUnitId: number | null | undefined;
    created: string;
    updated: string;
}