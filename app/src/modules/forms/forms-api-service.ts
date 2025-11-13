import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Form} from '../../models/entities/form';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {ApiOptions} from '../../services/api-service';
import {Page} from '../../models/dtos/page';
import {FormRevision} from '../../models/entities/form-revision';
import {CustomerInput} from '../../models/customer-input';
import {FormCostCalculationResponseDTO} from './dtos/form-cost-calculation-response-dto';
import {FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {EntityLockDto} from '../../models/dtos/entity-lock-dto';
import {SubmissionListResponseDTO} from '../submissions/dtos/submission-list-response-dto';
import {FormCitizenListResponseDTO} from './dtos/form-citizen-list-response-dto';
import {FormPublishChecklistItem} from './dtos/form-publish-checklist-item';
import {FormType} from './enums/form-type';
import {IdentityProviderInfo} from '../identity/models/identity-provider-info';
import {IdentityIdHeader} from '../identity/constants/identity-id-header';
import {ElementData, ElementDerivationResponse} from '../../models/element-data';
import {FormListResponseDTO} from './dtos/form-list-response-dto';
import {FormDetailsResponseDTO} from './dtos/form-details-response-dto';
import {FormCitizenDetailsResponseDTO} from './dtos/form-citizen-details-response-dto';
import {FormRequestDTO} from './dtos/form-request-dto';
import {RootElement} from '../../models/elements/root-element';
import {SortOrder} from '../../components/generic-list/generic-list-props';
import {FormStatus} from './enums/form-status';

interface FormFilters {
    id: number;
    slug: string;
    internalTitle: string;
    publicTitle: string;
    developingDepartmentId: number;
    managingDepartmentId: number;
    responsibleDepartmentId: number;
    publishedVersion: number;
    draftedVersion: number;
    formId: number;
    version: number;
    type: FormType;
    legalSupportDepartmentId: number;
    technicalSupportDepartmentId: number;
    imprintDepartmentId: number;
    privacyDepartmentId: number;
    accessibilityDepartmentId: number;
    destinationId: number;
    themeId: number;
    pdfTemplateKey: string;
    paymentProviderKey: string;
    identityVerificationRequired: boolean;
    identityProviderKey: string;
    isPublished: boolean;
    isRevoked: boolean;
    isDrafted: boolean;
    isCurrentlyPublishedVersion: boolean;
    isCurrentlyDraftedVersion: boolean;
    userId: string;
    isDeveloper: boolean;
    isManager: boolean;
    isResponsible: boolean;
}

export type DerivationSkipIdentifier = string[] | ['ALL'];

export type FormIdentifier = {
    id: number;
    version: number;
}

/**
 * @deprecated use FormsApiService from modules/forms/forms-api-service-v2.ts instead
 */
export class FormsApiService extends CrudApiService<FormRequestDTO, FormListResponseDTO, FormCitizenListResponseDTO, FormDetailsResponseDTO, FormCitizenDetailsResponseDTO, FormIdentifier, FormFilters> {
    public constructor(api: Api) {
        super(api, 'forms/');
    }

    public buildPath(id: FormIdentifier): string {
        return `${this.path}${id.id}/${id.version}/`;
    }

    public initialize(): FormDetailsResponseDTO {
        return FormsApiService.initialize();
    }

    public static initialize(): FormDetailsResponseDTO {
        return {
            id: 0,
            slug: '',
            version: 0,
            internalTitle: '',
            publicTitle: '',
            status: FormStatus.Drafted,
            type: FormType.Public,
            rootElement: generateElementWithDefaultValues(ElementType.Root) as RootElement,
            destinationId: null,
            legalSupportDepartmentId: null,
            technicalSupportDepartmentId: null,
            imprintDepartmentId: null,
            privacyDepartmentId: null,
            accessibilityDepartmentId: null,
            developingDepartmentId: 0,
            managingDepartmentId: null,
            responsibleDepartmentId: null,
            themeId: null,
            created: new Date().toISOString(),
            updated: new Date().toISOString(),
            customerAccessHours: 4,
            submissionRetentionWeeks: 4,
            pdfTemplateKey: null,
            paymentProducts: [],
            paymentPurpose: '',
            paymentDescription: '',
            paymentProviderKey: null,
            identityVerificationRequired: false,
            identityProviders: [],
            publishedVersion: null,
            draftedVersion: null,
            versionCount: 0,
            formId: 0,
            published: null,
            revoked: null,
        };
    }

    public async destroyAll(formId: number): Promise<void> {
        return await this.api.destroy(`forms/${formId}/`);
    }

    public async listRevisions(id: FormIdentifier, options?: ApiOptions): Promise<Page<FormRevision>> {
        return await this.api.get<Page<FormRevision>>(`forms/${id.id}/${id.version}/revisions/`, options);
    }

    public async listAllVersions(
        filters?: Partial<FormFilters>,
    ) {
        return await this.listVersions(0, 999, undefined, undefined, filters);
    }

    public async listVersions(
        page: number,
        limit: number,
        sort?: keyof FormDetailsResponseDTO,
        order?: SortOrder,
        filters?: Partial<FormFilters>,
    ): Promise<Page<FormDetailsResponseDTO>> {
        return await this.api.get<Page<FormDetailsResponseDTO>>(`form-versions/`, {
            queryParams: {
                page: page,
                size: limit,
                sort: sort != null && order != null ? `${sort},${order}` : undefined,
                ...filters,
            },
        });
    }

    // TODO: Remove usage of useFormsApi().rollbackRevision and use this instead
    public async rollbackRevision(formId: FormIdentifier, revisionId: number, options?: ApiOptions): Promise<Form> {
        return await this.api.get<Form>(`forms/${formId.id}/${formId.version}/revisions/rollback/${revisionId}/`, options);
    }

    public async calculateCosts(slug: string, version: number, customerInput: CustomerInput): Promise<FormCostCalculationResponseDTO> {
        return await this.api.post<FormCostCalculationResponseDTO>(`public/forms/${slug}/costs/`, customerInput, {queryParams: {version: version}});
    }

    public async submit(id: FormIdentifier, userInput: CustomerInput, identityId: string | undefined): Promise<SubmissionListResponseDTO> {
        const data = new FormData();
        data.set('inputs', JSON.stringify(userInput));


        const files: FileUploadElementItem[] = [];

        function processElementData(ed: ElementData) {
            for (const key of Object.keys(ed)) {
                const dataObject = ed[key];
                if (dataObject == null) {
                    return;
                }

                if (dataObject.$type === ElementType.FileUpload) {
                    const input: FileUploadElementItem[] | any = dataObject.inputValue ?? [];
                    if (Array.isArray(input)) {
                        files.push(...input);
                    }
                }

                if (dataObject.$type === ElementType.ReplicatingContainer) {
                    const items: ElementData[] = dataObject.inputValue ?? [];
                    if (Array.isArray(items)) {
                        for (const item of items) {
                            processElementData(item);
                        }
                    }
                }
            }
        }

        processElementData(userInput);

        for (const file of files) {
            const blob = await fetch(file.uri).then((r) => r.blob());
            data.append('files', blob, file.name);
        }

        return await this.api.postFormData<SubmissionListResponseDTO>(`public/submit/${id.id}/${id.version}/`, data, identityId != null ? {
            requestOptions: {
                headers: {
                    [IdentityIdHeader]: identityId ?? undefined,
                },
            },
        } : undefined);
    }

    public async sendApplicationCopy(submissionId: string, email: string): Promise<string> {
        return await this.api.post<string>(`public/send-copy/${submissionId}/`, {
            email,
        });
    }

    public async rateApplication(submissionId: string, score: number): Promise<string> {
        return await this.api.getPublic<string>(
            `rate/${submissionId}/`, {queryParams: {score: score.toFixed(0)}},
        );
    }

    public async getLockState(id: number): Promise<EntityLockDto> {
        return await this.api.get<EntityLockDto>(`forms/${id}/lock/`);
    }

    public async deleteLockState(id: number): Promise<void> {
        return await this.api.destroy<void>(`forms/${id}/lock/`);
    }

    public async getMaxFileSize(slug: string, version?: number | undefined) {
        return await this.api.getPublic<{ maxFileSize: number }>(`forms/${slug}/max-file-size/?${version != null ? `version=${version}` : ''}`);
    }

    public async getIdentityProviders(slug: string, version?: number | undefined): Promise<Page<IdentityProviderInfo>> {
        return await this.api.getPublic(`forms/${slug}/identity-providers/?${version != null ? `version=${version}` : ''}`);
    }

    public latestAsNewVersion(id: number): Promise<FormDetailsResponseDTO> {
        return this.api.put<FormDetailsResponseDTO>(`forms/${id}/latest/as-new-version/`, {});
    }

    public versionAsNewVersion({id, version}: FormIdentifier): Promise<FormDetailsResponseDTO> {
        return this.api.put<FormDetailsResponseDTO>(`forms/${id}/${version}/as-new-version/`, {});
    }

    public publish({id, version}: FormIdentifier): Promise<Form> {
        return this.api.put<Form>(`forms/${id}/${version}/publish/`, {});
    }

    public revoke({id, version}: FormIdentifier): Promise<Form> {
        return this.api.put<Form>(`forms/${id}/${version}/revoke/`, {});
    }

    public checkPublish({id, version}: FormIdentifier): Promise<FormPublishChecklistItem[]> {
        return this.api.get<FormPublishChecklistItem[]>(`forms/${id}/${version}/check-publish/`);
    }

    public checkSlugExists(slug: string): Promise<boolean> {
        return this.api.get<boolean>(`form-slugs/${slug}/`);
    }

    public xdfTransform(value: string | ArrayBuffer): Promise<FormDetailsResponseDTO> {
        return this.api.postXML<FormDetailsResponseDTO>('xdf/v2/transform/', value);
    }
}