export interface DepartmentRequestDTO {
    name: string;
    address: string;
    imprint: string;
    privacy: string;
    accessibility: string;
    technicalSupportAddress: string;
    specialSupportAddress: string;
    departmentMail?: string | null;
}
