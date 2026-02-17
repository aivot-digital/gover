export interface DepartmentEntity {
    id: number;
    parentDepartmentId?: number | null;
    depth: number;
    name: string;
    address?: string | null;
    imprint?: string | null;
    commonPrivacy?: string | null;
    commonAccessibility?: string | null;
    technicalSupportAddress?: string | null;
    technicalSupportPhone?: string | null;
    technicalSupportInfo?: string | null;
    specialSupportAddress?: string | null;
    specialSupportPhone?: string | null;
    specialSupportInfo?: string | null;
    additionalInfo?: string | null;
    departmentMail?: string | null;
    themeId?: number | null;
    created: string;
    updated: string;
}

