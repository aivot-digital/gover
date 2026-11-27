export interface DashboardOverview {
    tasks: DashboardTaskSummary;
    instances: DashboardInstanceSummary;
    recentProcesses: DashboardRecentProcess[];
}

export interface DashboardTaskSummary {
    total: number;
    overdue: number;
    items: DashboardTask[];
}

export interface DashboardTask {
    id: number;
    processInstanceId: number;
    processId: number;
    processVersion: number;
    taskName: string;
    processTitle: string;
    caseNumber: string;
    started: string;
    deadline: string | null;
}

// Running process instances personally assigned to the current user.
export interface DashboardInstanceSummary {
    total: number;
    overdue: number;
    items: DashboardInstance[];
}

export interface DashboardInstance {
    id: number;
    processId: number;
    processVersion: number;
    processTitle: string;
    caseNumber: string;
    started: string;
    deadline: string | null;
}

export interface DashboardRecentProcess {
    id: number;
    title: string;
    draftedVersion: number | null;
    publishedVersion: number | null;
    updated: string;
}

export interface DashboardActivity {
    available: boolean;
    period: DashboardActivityPeriod;
    started: number;
    completed: number;
    active: number;
    buckets: DashboardActivityBucket[];
}

export enum DashboardActivityPeriod {
    ThirtyDays = 'ThirtyDays',
    ThreeMonths = 'ThreeMonths',
}

// System configs persist the period with the backend's stable numeric assignment.
export enum DashboardActivityPeriodConfig {
    ThirtyDays = '0',
    ThreeMonths = '1',
}

export interface DashboardActivityBucket {
    periodStart: string;
    started: number;
    completed: number;
}
