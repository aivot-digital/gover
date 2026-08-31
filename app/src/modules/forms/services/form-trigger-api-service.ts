import type {Page} from '../../../models/dtos/page';
import type {AuthoredElementValues, DerivedRuntimeElementData} from '../../../models/element-data';
import type {FormLayoutElement} from '../../../models/elements/form-layout-element';
import type {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import type {SortOrder} from '../../../components/generic-list/generic-list-props';
import type {QueryParams} from '../../../services/base-api-service';
import {BaseApiService} from '../../../services/base-api-service';
import type {ProcessEntity} from '../../process/entities/process-entity';
import type {ProcessNodeEntity} from '../../process/entities/process-node-entity';
import type {ProcessVersionEntity} from '../../process/entities/process-version-entity';
import type {Theme} from '../../themes/models/theme';
import type {FormTriggerIdentityDetailsDTO} from '../dtos/form-trigger-identity-details-dto';
import type {PaymentConfigElementValue} from '../../../models/elements/form/input/payment-config-element';
import type {IdentityProviderType} from '../../identity/enums/identity-provider-type';

export interface FormTriggerFilter {
    id: number;
    notId: number;
    name: string;
    processId: number;
    processVersion: number;
    dataKey: string;
    formSlug: string;
}

export type FormTriggerSortField =
    keyof Pick<ProcessNodeEntity, 'id' | 'name' | 'processId' | 'processVersion' | 'dataKey' | 'savedWithErrors' | 'updated'>;

export interface FormTriggerConfiguration extends AuthoredElementValues {
    formSlug?: string;
    formLayout?: FormLayoutElement;
    identityProviders?: Array<Record<string, unknown>>;
    payment?: PaymentConfigElementValue;
}

export interface FormTriggerNodeEntity extends Omit<ProcessNodeEntity, 'configuration'> {
    configuration: FormTriggerConfiguration;
}

export interface FormTriggerListItem {
    process: ProcessEntity;
    version: ProcessVersionEntity;
    node: FormTriggerNodeEntity;
}

export interface FormTriggerSubmissionStatusResponseV1 {
    startedProcessAccessKey: string;
}

export interface FormTriggerCostCalculationResponseV1 {
    totalCost: number;
    hasTaxes: boolean;
    paymentItems: {
        id: string;
        reference: string;
        description: string;
        quantity: number;
        taxRate: number;
        netPrice: number;
        totalPrice: number;
        bookingData: {
            key: string;
            value: string;
        }[];
        taxInformation: string;
    }[];
    paymentProviderName: string;
}

export type FormOverviewMode = 'Published' | 'Drafted';

export interface FormOverviewItem {
    id: number;
    nodeName: string;
    formTitle: string;
    processId: number;
    processTitle: string;
    processVersion: number;
    status: FormOverviewMode;
    publicUrl: string | null;
    showOnFormIndexPage: boolean;
    updated: string;
    published: string | null;
}

export type FormIdentityType = 'IdentityProvider' | 'Email';

export interface FormIdentityCommunicationChoice {
    id: number;
    name: string;
    description: string;
}

export interface FormIdentityCommunicationState {
    required: boolean;
    ready: boolean;
    selectedBindingId: number | null;
    choices: FormIdentityCommunicationChoice[];
    customerLayout: GroupLayout | null;
    customerData: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
}

export interface FormIdentitySlot {
    id: string;
    title: string | null;
    description: string | null;
    isOptional: boolean;
    isRequired: boolean;
    allowsEmail: boolean;
    identityType: FormIdentityType | null;
    emailAddress: string | null;
    isReady: boolean;
    availableIdentityProviders: {
        identityProviderKey: string;
        identityProviderName: string;
        identityProviderAssetKey: string | null;
        identityProviderType: IdentityProviderType;
        isAuthenticatedWithThis: boolean;
        additionalScopes: string[];
    }[];
    communication: FormIdentityCommunicationState | null;
}

interface FormIdentitySlotsResponse {
    identitySlots: FormIdentitySlot[];
}

export class FormTriggerApiService extends BaseApiService {
    public async getIdentitySlots(
        processSlug: string,
        formSlug: string,
        testClaim?: string,
    ): Promise<FormIdentitySlot[]> {
        const response = await this.get<FormIdentitySlotsResponse>(
            `/api/public/form/${encodeURIComponent(processSlug)}/${encodeURIComponent(formSlug)}/`,
            {
                query: {'test-claim': testClaim},
                skipAuthCheck: true,
            },
        );

        return response.identitySlots;
    }

    public createIdentityProviderStartLink(
        processSlug: string,
        formSlug: string,
        identityId: string,
        providerKey: string,
        testClaim?: string,
        origin?: string,
    ): string {
        return this.createPath(
            `/api/public/form/${encodeURIComponent(processSlug)}/${encodeURIComponent(formSlug)}/identities/${encodeURIComponent(identityId)}/providers/${encodeURIComponent(providerKey)}/start/`,
            {
                origin: origin ?? window.location.href,
                'test-claim': testClaim,
            },
        );
    }

    public setEmailIdentity(
        processSlug: string,
        formSlug: string,
        identityId: string,
        emailAddress: string,
        testClaim?: string,
    ): Promise<FormIdentitySlot> {
        return this.put(
            `/api/public/form/${encodeURIComponent(processSlug)}/${encodeURIComponent(formSlug)}/identities/${encodeURIComponent(identityId)}/email/`,
            {emailAddress},
            {query: {'test-claim': testClaim}, skipAuthCheck: true},
        );
    }

    public clearIdentity(
        processSlug: string,
        formSlug: string,
        identityId: string,
        testClaim?: string,
    ): Promise<void> {
        return this.delete(
            `/api/public/form/${encodeURIComponent(processSlug)}/${encodeURIComponent(formSlug)}/identities/${encodeURIComponent(identityId)}/`,
            {query: {'test-claim': testClaim}, skipAuthCheck: true},
        );
    }

    public selectCommunication(
        identityId: string,
        relatedProcessNodeId: number,
        bindingId: number,
        customerData: AuthoredElementValues,
    ): Promise<FormIdentityCommunicationState> {
        return this.put(
            `/api/public/identity/${encodeURIComponent(identityId)}/communication/`,
            {bindingId, customerData},
            {query: {relatedProcessNodeId}, skipAuthCheck: true},
        );
    }

    public deriveCommunication(
        identityId: string,
        relatedProcessNodeId: number,
        bindingId: number,
        customerData: AuthoredElementValues,
    ): Promise<FormIdentityCommunicationState> {
        return this.post(
            `/api/public/identity/${encodeURIComponent(identityId)}/communication/derive/`,
            {bindingId, customerData},
            {query: {relatedProcessNodeId}, skipAuthCheck: true},
        );
    }

    public async submitForm(processSlug: string, triggerSlug: string, formData: FormData, options?: {
        testClaim?: string;
    }): Promise<FormTriggerSubmissionStatusResponseV1> {
        return await this.postFormData<FormTriggerSubmissionStatusResponseV1>(
            `/api/public/form/${processSlug}/${triggerSlug}/submit/`,
            formData,
            {
                query: {
                    'test-claim': options?.testClaim,
                },
            },
        );
    }

    public async calculateCosts(processSlug: string, triggerSlug: string, authoredElementValues: AuthoredElementValues, options?: {
        testClaim?: string;
    }): Promise<FormTriggerCostCalculationResponseV1> {
        return await this.post<AuthoredElementValues, FormTriggerCostCalculationResponseV1>(
            `/api/public/form/${processSlug}/${triggerSlug}/costs/`,
            authoredElementValues,
            {
                query: {
                    'test-claim': options?.testClaim,
                },
            },
        );
    }


    public async listOverview(
        page: number,
        limit: number,
        view: FormOverviewMode,
        search?: string,
        sort?: FormTriggerSortField | FormTriggerSortField[],
        order?: SortOrder,
    ): Promise<Page<FormOverviewItem>> {
        return await this.get<Page<FormOverviewItem>>('/api/forms/v1/', {
            query: {
                ...this.buildListQuery(page, limit, sort, order),
                view,
                search,
            },
        });
    }

    public async listPublic(
        page: number,
        limit: number,
        sort?: FormTriggerSortField | FormTriggerSortField[],
        order?: SortOrder,
        filters?: Partial<FormTriggerFilter>,
    ): Promise<Page<FormTriggerListItem>> {
        return await this.get<Page<FormTriggerListItem>>('/api/public/forms/', {
            query: this.buildListQuery(page, limit, sort, order, filters),
            skipAuthCheck: true,
        });
    }

    public async listPublicAll(filters?: Partial<FormTriggerFilter>): Promise<Page<FormTriggerListItem>> {
        return await this.listPublic(0, 999, undefined, undefined, filters);
    }

    public async getIdentityProviders(): Promise<FormTriggerIdentityDetailsDTO[]> {
        return await this.get<FormTriggerIdentityDetailsDTO[]>('/api/public/identity/providers/', {
            skipAuthCheck: true,
        });
    }

    public async getFormTheme(
        processSlug: string,
        formSlug: string,
        version?: number,
        testClaimAccessKey?: string,
    ): Promise<Theme> {
        return await this.get<Theme>(`/api/public/form/${processSlug}/${formSlug}/theme/`, {
            query: {
                version,
                'test-claim': testClaimAccessKey,
            },
            skipAuthCheck: true,
        });
    }

    public async downloadPrintablePdf(nodeId: number): Promise<Blob> {
        return await this.getBlob(`/api/forms/v1/${nodeId}/print-pdf/`);
    }

    public async downloadSubmittedSummaryPdf(
        processSlug: string,
        triggerSlug: string,
        instanceAccessKey: string,
        taskAccessKey: string,
        version?: number,
    ): Promise<Blob> {
        return await this.getBlob(
            `/api/public/form/${processSlug}/${triggerSlug}/submit/${encodeURIComponent(instanceAccessKey)}/${encodeURIComponent(taskAccessKey)}/print/`,
            {
                query: {
                    version,
                },
                skipAuthCheck: true,
            },
        );
    }

    public async downloadPaymentConfirmationPdf(
        processSlug: string,
        triggerSlug: string,
        instanceAccessKey: string,
        taskAccessKey: string,
        version?: number,
    ): Promise<Blob> {
        return await this.getBlob(
            `/api/public/form/${processSlug}/${triggerSlug}/submit/${encodeURIComponent(instanceAccessKey)}/${encodeURIComponent(taskAccessKey)}/payment-confirmation/`,
            {
                query: {
                    version,
                },
                skipAuthCheck: true,
            },
        );
    }

    private buildListQuery(
        page: number,
        limit: number,
        sort?: FormTriggerSortField | FormTriggerSortField[],
        order?: SortOrder,
        filters?: Partial<FormTriggerFilter>,
    ): QueryParams {
        const {formSlug, ...restFilters} = filters ?? {};
        let sortQuery: string | string[] | undefined;
        if (sort != null && order != null) {
            if (Array.isArray(sort)) {
                sortQuery = sort.map((field) => `${field},${order}`);
            } else {
                sortQuery = `${sort},${order}`;
            }
        }

        const query: QueryParams = {
            page,
            size: limit,
            sort: sortQuery,
            ...restFilters,
        };
        if (formSlug != null) {
            query['configEquals[formSlug]'] = formSlug;
        }

        return query;
    }
}
