export interface DepartmentResponseDTO {
    id: number;
    name: string;
    address: string;
    imprint: string;
    privacy: string;
    accessibility: string;
    technicalSupportAddress: string;
    specialSupportAddress: string;
    departmentMail?: string | null;
    created: string;
    updated: string;
}