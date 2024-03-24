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
    created: string;
    updated: string;
}

export function NewDepartment(name?: string): Department {
    return {
        id: 0,
        name: name ?? '',
        address: '',
        imprint: '',
        privacy: '',
        accessibility: '',
        technicalSupportAddress: '',
        specialSupportAddress: '',
        created: new Date().toISOString(),
        updated: new Date().toISOString(),
    };
}