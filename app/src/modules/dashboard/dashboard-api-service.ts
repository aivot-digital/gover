import {BaseApiService} from '../../services/base-api-service';
import {DashboardStatItem} from './models/dashboard-stat-item';

export class DashboardApiService extends BaseApiService {
    public async fetchStats(): Promise<DashboardStatItem[]> {
        return this.get<DashboardStatItem[]>('/api/system/dashboard/stats/');
    }
}