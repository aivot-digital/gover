import {BaseApiService} from '../../services/base-api-service';
import {type DashboardActivity, type DashboardOverview, type DashboardTaskSummary} from './models/dashboard-overview';

export class DashboardApiService extends BaseApiService {
    public fetchOverview(): Promise<DashboardOverview> {
        return this.get<DashboardOverview>('/api/system/dashboard/overview/');
    }

    public fetchTasks(): Promise<DashboardTaskSummary> {
        return this.get<DashboardTaskSummary>('/api/system/dashboard/tasks/');
    }

    public fetchActivity(): Promise<DashboardActivity> {
        return this.get<DashboardActivity>('/api/system/dashboard/activity/');
    }
}
