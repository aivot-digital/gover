import type {Page} from '../../../models/dtos/page';
import type {AuthoredElementValues} from '../../../models/element-data';
import type {FormLayoutElement} from '../../../models/elements/form-layout-element';
import type {SortOrder} from '../../../components/generic-list/generic-list-props';
import {BaseApiService} from '../../../services/base-api-service';
import type {QueryParams} from '../../../services/base-api-service';
import type {ProcessEntity} from '../../process/entities/process-entity';
import type {ProcessNodeEntity} from '../../process/entities/process-node-entity';
import type {ProcessVersionEntity} from '../../process/entities/process-version-entity';
import type {Theme} from '../../themes/models/theme';
import type {FormTriggerIdentityDetailsDTO} from '../dtos/form-trigger-identity-details-dto';

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
}

export interface FormTriggerNodeEntity extends Omit<ProcessNodeEntity, 'configuration'> {
    configuration: FormTriggerConfiguration;
}

export interface FormTriggerListItem {
    process: ProcessEntity;
    version: ProcessVersionEntity;
    node: FormTriggerNodeEntity;
}

export class FormTriggerApiService extends BaseApiService {
    public async list(
        page: number,
        limit: number,
        sort?: FormTriggerSortField | FormTriggerSortField[],
        order?: SortOrder,
        filters?: Partial<FormTriggerFilter>,
    ): Promise<Page<FormTriggerListItem>> {
        return await this.get<Page<FormTriggerListItem>>('/api/forms/v1/', {
            query: this.buildListQuery(page, limit, sort, order, filters),
        });
    }

    public async listAll(filters?: Partial<FormTriggerFilter>): Promise<Page<FormTriggerListItem>> {
        return await this.list(0, 999, undefined, undefined, filters);
    }

    public async listPublic(
        page: number,
        limit: number,
        sort?: FormTriggerSortField | FormTriggerSortField[],
        order?: SortOrder,
        filters?: Partial<FormTriggerFilter>,
    ): Promise<Page<FormTriggerListItem>> {
        return await this.get<Page<FormTriggerListItem>>('/api/public/forms/v1/', {
            query: this.buildListQuery(page, limit, sort, order, filters),
            skipAuthCheck: true,
        });
    }

    public async listPublicAll(filters?: Partial<FormTriggerFilter>): Promise<Page<FormTriggerListItem>> {
        return await this.listPublic(0, 999, undefined, undefined, filters);
    }

    public async getIdentityProviders(): Promise<FormTriggerIdentityDetailsDTO[]> {
        return await this.get<FormTriggerIdentityDetailsDTO[]>(`/api/public/identity/providers/`, {
            skipAuthCheck: true,
        });
    }

    public async getFormTheme(
        processAccessKey: string,
        formSlug: string,
        version?: number,
        testClaimAccessKey?: string,
    ): Promise<Theme> {
        return await this.get<Theme>(`/api/public/forms/v1/${processAccessKey}/${formSlug}/theme/`, {
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
