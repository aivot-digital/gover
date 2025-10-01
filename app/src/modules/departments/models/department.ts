export interface Department {
    id: number;
    name: string;
    address: string;
    imprint: string;
    privacy: string;
    accessibility: string;
    technicalSupportAddress: string;
    specialSupportAddress: string;
    departmentMail?: string | null;
    themeId?: number | null;
    created: string;
    updated: string;
}