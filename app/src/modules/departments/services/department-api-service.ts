import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {DepartmentEntity} from '../entities/department-entity';
import {PublicDepartmentResponseDTO} from '../entities/v-department-shadowed-entity';

// The help, privacy, imprint and accessibility dialogs and the address section of a form load their departments at
// once, often the same one; they share the request while it is pending.
const pendingPublicRequests = new Map<number, Promise<PublicDepartmentResponseDTO>>();

interface DepartmentFilter {
    id: number;
    ids: number[];
    name: string;
    themeId: number;
}

export class DepartmentApiService extends BaseCrudApiService<DepartmentEntity, DepartmentEntity, DepartmentEntity, DepartmentEntity, number, DepartmentFilter> {
    public constructor() {
        super('api/departments/');
    }

    public initialize(): DepartmentEntity {
        return DepartmentApiService.initialize();
    }

    public static initialize(): DepartmentEntity {
        return {
            postalAddress: undefined,
            commonAccessibility: undefined,
            commonPrivacy: undefined,
            created: new Date().toISOString(),
            defaultMailSignature: undefined,
            depth: 0,
            id: 0,
            imprint: undefined,
            name: '',
            parentDepartmentId: undefined,
            specialSupportEmail: undefined,
            specialSupportInfo: undefined,
            specialSupportPhone: undefined,
            technicalSupportEmail: undefined,
            technicalSupportInfo: undefined,
            technicalSupportPhone: undefined,
            themeId: undefined,
            updated: new Date().toISOString(),
        };
    }

    public retrievePublic(id: number): Promise<PublicDepartmentResponseDTO> {
        const pendingRequest = pendingPublicRequests.get(id);
        if (pendingRequest != null) {
            return pendingRequest;
        }

        const request = this
            .get<PublicDepartmentResponseDTO>(`/api/public/departments/${id}/`)
            .finally(() => {
                pendingPublicRequests.delete(id);
            });
        pendingPublicRequests.set(id, request);
        return request;
    }
}
