import {BaseApiService} from '../../../services/base-api-service';
import {ProcessInstanceStatus} from '../../../modules/process/enums/process-instance-status';
import {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import {ProcessTaskStatus} from '../../../modules/process/enums/process-task-status';
import {isApiError} from '../../../models/api-error';
import type {IdentityCommunicationState, IdentitySlot} from '../../../modules/identity/models/identity-slot';
import type {IdentitySelectionApi} from '../../../modules/identity/models/identity-selection-api';
import type {CustomerTaskViewResponse} from '../../../modules/process/models/customer-task-view';

export type {CustomerTaskViewResponse as TaskViewResponse} from '../../../modules/process/models/customer-task-view';

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
    public createIdentitySelectionApi(
        instanceAccessKey: string,
        taskAccessKey: string,
    ): IdentitySelectionApi {
        return {
            createIdentityProviderStartLink: (identityId, providerKey, origin) => (
                this.createNewIdentityProviderStartLink(
                    instanceAccessKey,
                    taskAccessKey,
                    identityId,
                    providerKey,
                    origin,
                )
            ),
            setEmailIdentity: (identityId, emailAddress) => (
                this.setNewIdentityEmail(
                    instanceAccessKey,
                    taskAccessKey,
                    identityId,
                    emailAddress,
                )
            ),
            clearIdentity: (identityId) => (
                this.clearNewIdentity(instanceAccessKey, taskAccessKey, identityId)
            ),
            selectCommunication: (identityId, bindingId, customerData) => (
                this.selectNewIdentityCommunication(
                    instanceAccessKey,
                    taskAccessKey,
                    identityId,
                    bindingId,
                    customerData,
                )
            ),
            deriveCommunication: (identityId, bindingId, customerData) => (
                this.deriveNewIdentityCommunication(
                    instanceAccessKey,
                    taskAccessKey,
                    identityId,
                    bindingId,
                    customerData,
                )
            ),
        };
    }

    public async getInstanceStatus(instanceAccessKey: string): Promise<ProcessInstanceStatusResponse> {
        return await this.get<ProcessInstanceStatusResponse>(`/api/public/processes/${instanceAccessKey}/`, {
            skipAuthCheck: true,
        });
    }

    public async getTaskView(instanceAccessKey: string, taskAccessKey: string): Promise<CustomerTaskViewResponse> {
        return await this.get<CustomerTaskViewResponse>(`/api/public/processes/${instanceAccessKey}/tasks/${taskAccessKey}/`, {
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

    public createNewIdentityProviderStartLink(
        instanceAccessKey: string,
        taskAccessKey: string,
        identityId: string,
        providerKey: string,
        origin: string = window.location.href,
    ): string {
        const cleanedOrigin = new URL(removeIdentityCallbackParameters(origin));
        const queryParameters = new URLSearchParams(cleanedOrigin.search);
        queryParameters.set('origin', cleanedOrigin.toString());

        return this.createPath(
            `/api/public/processes/${encodeURIComponent(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}/identities/${encodeURIComponent(identityId)}/providers/${encodeURIComponent(providerKey)}/start/`,
            queryParameters,
        );
    }

    public setNewIdentityEmail(
        instanceAccessKey: string,
        taskAccessKey: string,
        identityId: string,
        emailAddress: string,
    ): Promise<IdentitySlot> {
        return this.put(
            `/api/public/processes/${encodeURIComponent(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}/identities/${encodeURIComponent(identityId)}/email/`,
            {emailAddress},
            {skipAuthCheck: true, doNotHandleStatusCodes: true},
        );
    }

    public clearNewIdentity(
        instanceAccessKey: string,
        taskAccessKey: string,
        identityId: string,
    ): Promise<void> {
        return this.delete(
            `/api/public/processes/${encodeURIComponent(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}/identities/${encodeURIComponent(identityId)}/`,
            {skipAuthCheck: true, doNotHandleStatusCodes: true},
        );
    }

    public selectNewIdentityCommunication(
        instanceAccessKey: string,
        taskAccessKey: string,
        identityId: string,
        bindingId: number,
        customerData: AuthoredElementValues,
    ): Promise<IdentityCommunicationState> {
        return this.put(
            `/api/public/processes/${encodeURIComponent(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}/identities/${encodeURIComponent(identityId)}/communication/`,
            {bindingId, customerData},
            {skipAuthCheck: true, doNotHandleStatusCodes: true},
        );
    }

    public deriveNewIdentityCommunication(
        instanceAccessKey: string,
        taskAccessKey: string,
        identityId: string,
        bindingId: number,
        customerData: AuthoredElementValues,
    ): Promise<IdentityCommunicationState> {
        return this.post(
            `/api/public/processes/${encodeURIComponent(instanceAccessKey)}/tasks/${encodeURIComponent(taskAccessKey)}/identities/${encodeURIComponent(identityId)}/communication/derive/`,
            {bindingId, customerData},
            {skipAuthCheck: true, doNotHandleStatusCodes: true},
        );
    }
}
