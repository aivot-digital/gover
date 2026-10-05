export interface DepartmentEntity {
    id: number;
    parentDepartmentId?: number | null;
    depth: number;
    name: string;
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
    created: string;
    updated: string;
}
