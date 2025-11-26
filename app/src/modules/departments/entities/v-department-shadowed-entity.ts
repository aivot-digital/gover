export interface VDepartmentShadowedEntity {
    id: number;
    name: string;
    address?: string | null;
    imprint?: string | null;
    commonPrivacy?: string | null;
    commonAccessibility?: string | null;
    technicalSupportAddress?: string | null;
    specialSupportAddress?: string | null;
    created: string;
    updated: string;
    departmentMail?: string | null;
    themeId?: number | null;
    technicalSupportPhone?: string | null;
    technicalSupportInfo?: string | null;
    specialSupportPhone?: string | null;
    specialSupportInfo?: string | null;
    additionalInfo?: string | null;
    depth: number;
    parentDepartmentId?: number | null;
    parentNames?: string[] | null;
    parentIds?: number[] | null;
}

export interface VDepartmentShadowedEntityWithChildren extends VDepartmentShadowedEntity {
    children: VDepartmentShadowedEntityWithChildren[];
}
