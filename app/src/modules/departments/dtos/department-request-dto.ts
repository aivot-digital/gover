export interface DepartmentRequestDTO {
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
    parentOrgUnitId: number | null | undefined;
}
