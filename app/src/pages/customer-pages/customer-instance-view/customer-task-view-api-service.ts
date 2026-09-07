import {BaseApiService} from '../../../services/base-api-service';
import {ProcessInstanceStatus} from '../../../modules/process/enums/process-instance-status';
import {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import {TaskViewEvent} from '../../../modules/process/services/process-instance-task-api-service';
import {ProcessTaskStatus} from '../../../modules/process/enums/process-task-status';
import {isApiError} from '../../../models/api-error';

export const REQUIRED_IDENTITY_AUTHENTICATION_REASON = 'required_identity_authentication';

export interface ProcessInstanceStatusResponse {
    title: string;
    status: ProcessInstanceStatus;
    statusOverride: string;
    tasks: ProcessInstanceTaskStatusResponse[] | null;
    accessibilityDepartmentId: number | null;
    privacyDepartmentId: number | null;
    imprintDepartmentId: number | null;
    legalSupportDepartmentId: number | null;
    technicalSupportDepartmentId: number | null;
}

export interface ProcessInstanceTaskStatusResponse {
    accessKey: string;
    status: ProcessTaskStatus;
    statusOverride: string;
}

export interface TaskViewResponse {
    layout: GroupLayout;
    data: AuthoredElementValues;
    events: Array<TaskViewEvent>;
}

export function getActiveCustomerTasks(
    tasks: ProcessInstanceTaskStatusResponse[] | null | undefined,
): ProcessInstanceTaskStatusResponse[] {
    return (tasks ?? []).filter((task) => (
        task.status === ProcessTaskStatus.AwaitingCustomer ||
        task.status === ProcessTaskStatus.AwaitingPayment
    ));
}

export function buildCustomerInstancePath(instanceAccessKey: string): string {
    return `/process/${encodeURIComponent(instanceAccessKey)}`;
}

export function buildCustomerTaskPath(instanceAccessKey: string, taskAccessKey: string): string {
    return `${buildCustomerInstancePath(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}`;
}

export function isRequiredIdentityAuthenticationError(error: unknown): boolean {
    return isApiError(error) &&
        error.status === 401 &&
        error.details != null &&
        typeof error.details === 'object' &&
        !Array.isArray(error.details) &&
        error.details.reason === REQUIRED_IDENTITY_AUTHENTICATION_REASON;
}

export function removeIdentityCallbackParameters(rawUrl: string): string {
    const url = new URL(rawUrl);
    url.searchParams.delete('identity-state');
    url.searchParams.delete('error');
    url.searchParams.delete('error_description');
    return url.toString();
}

export class CustomerTaskViewApiService extends BaseApiService {
    public async getInstanceStatus(instanceAccessKey: string): Promise<ProcessInstanceStatusResponse> {
        return await this.get<ProcessInstanceStatusResponse>(`/api/public/processes/${instanceAccessKey}/`, {
            skipAuthCheck: true,
        });
    }

    public async getTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<TaskViewResponse> {
        return await this.get<TaskViewResponse>(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`, {
            skipAuthCheck: true,
            doNotHandleStatusCodes: true,
        });
    }

    public async deriveTaskView(instanceAccessKey: string, taskAccessKey: string, values: AuthoredElementValues, skipErrorsForElements: string[] = []): Promise<DerivedRuntimeElementData> {
        return await this.post(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/derive/`, values, {
            query: {
                // 'test-claim': testClaimKey, TODO: Add if necessary
                skipErrorsFor: skipErrorsForElements,
                skipVisibilitiesFor: [],
                skipValuesFor: [],
                skipOverridesFor: [],
            },
            skipAuthCheck: true,
            doNotHandleStatusCodes: true,
        });
    }

    public createRequiredIdentityAuthenticationStartLink(
        instanceAccessKey: string,
        taskAccessKey: string,
        origin: string = window.location.href,
    ): string {
        const cleanedOrigin = new URL(removeIdentityCallbackParameters(origin));
        const queryParameters = new URLSearchParams(cleanedOrigin.search);
        queryParameters.set('origin', cleanedOrigin.toString());

        return this.createPath(
            `/api/public/processes/${encodeURIComponent(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}/identity/start/`,
            queryParameters,
        );
    }
}
