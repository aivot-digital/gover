import {BaseApiService} from '../../services/base-api-service';
import {type PermissionProvider} from './models/permission-provider';
import {type PermissionSet} from './models/permission-set';

export class PermissionApiService extends BaseApiService {
    public async listPermissions(): Promise<PermissionProvider[]> {
        return await this.get<PermissionProvider[]>('/api/permissions/');
    }

    public async getPermissionSetForUser(userId: string): Promise<PermissionSet> {
        return await this.get<PermissionSet>(`/api/permissions/users/${userId}/`);
    }
}
