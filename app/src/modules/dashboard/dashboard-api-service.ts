import {BaseApiService} from '../../services/base-api-service';
import {type DashboardActivity, type DashboardOverview} from './models/dashboard-overview';

export class DashboardApiService extends BaseApiService {
    public fetchOverview(): Promise<DashboardOverview> {
        return this.get<DashboardOverview>('/api/system/dashboard/overview/');
    }

    public fetchActivity(): Promise<DashboardActivity> {
        return this.get<DashboardActivity>('/api/system/dashboard/activity/');
    }
}
