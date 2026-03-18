import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {FormEntity} from '../entities/form-entity';
import {FormExport} from '../entities/form-export';
import {FormEditor} from '../dtos/form-editor';
import {EntityLockDto} from '../../../models/dtos/entity-lock-dto';
import {Theme} from '../../themes/models/theme';
import {Page} from '../../../models/dtos/page';
import {IdentityProviderInfo} from '../../identity/models/identity-provider-info';
import {CustomerInput} from '../../../models/customer-input';
import {AuthoredElementValues, ElementDerivationResponse} from '../../../models/element-data';
import {RequestOptions} from '../../../services/base-api-service';
import {FormPublishChecklistItem} from '../dtos/form-publish-checklist-item';
import {VFormVersionWithDetailsEntity} from '../entities/v-form-version-with-details-entity';
import {FormVersionEntityId} from './form-version-api-service';
import {FormVersionEntity} from '../entities/form-version-entity';
import {ApiOptions} from '../../../services/api-service';
import {FormRevision} from '../../../models/entities/form-revision';
import type {FormCitizenDetailsResponseDTO} from '../dtos/form-citizen-details-response-dto';
import {IdentityIdHeader} from '../../identity/constants/identity-id-header';
import {FileUploadElementItem} from '../../../models/elements/form/input/file-upload-element';
import {SubmissionListResponseDTO} from '../../submissions/dtos/submission-list-response-dto';
import {ElementType} from '../../../data/element-type/element-type';
import {FormCostCalculationResponseDTO} from '../dtos/form-cost-calculation-response-dto';
import {FormCitizenListResponseDTO} from '../dtos/form-citizen-list-response-dto';
import {RootElement} from '../../../models/elements/root-element';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {isReplicatingContainerLayout} from '../../../models/elements/form/layout/replicating-container-layout';

export type DerivationSkipIdentifier = string[] | ['ALL'];

interface FormFilter {
    id: number;
    slug: string;
    internalTitle: string;
    developingDepartmentId: number;
    developingDepartmentIdNot: number;
    publishedVersion: number;
    draftedVersion: number;
    isDrafted: boolean;
    isPublished: boolean;
    isRevoked: boolean;
}

export class FormApiService extends BaseCrudApiService<FormEntity, FormEntity, FormEntity, FormEntity, number, FormFilter> {
    public constructor() {
        super('/api/forms/');
    }

    public initialize(): FormEntity {
        return FormApiService.initialize();
    }

    public static initialize(): FormEntity {
        return {
            id: 0,
            slug: '',
            internalTitle: '',
            developingDepartmentId: 0,
            publishedVersion: 0,
            draftedVersion: 0,
            versionCount: 0,
            created: '',
            updated: '',
        };
    }

    public async checkSlugExists(slug: string): Promise<boolean> {
        try {
            const res = await this.get<any>(`/api/public/forms/${slug}/`);
        } catch (_) {
            return false;
        }
        return true;
    }

    public async retrieveBySlugAndVersion(slug: string, version: string | undefined, identityId: string | undefined): Promise<FormCitizenDetailsResponseDTO> {
        const apiOptions: RequestOptions = {
            query: {
                version: version,
            },
            headers: identityId != null ? {
                [IdentityIdHeader]: identityId,
            } : undefined,
        };

        return await this.get<FormCitizenDetailsResponseDTO>(`/api/public/forms/${slug}/`, {
            ...apiOptions,
            skipAuthCheck: true,
        });
    }

    public listAllCitizenForms(): Promise<FormCitizenListResponseDTO[]> {
        return this
            .get<Page<FormCitizenListResponseDTO>>('/api/public/forms/', {
                query: {
                    page: 0,
                    size: 999,
                    sort: 'publicTitle,asc',
                },
                skipAuthCheck: true,
            })
            .then(({content}) => content);
    }

    public export(id: number, version?: number): Promise<FormExport> {
        return this.get<FormExport>(`${this.path}${id}/export/`, {
            query: {
                version: version,
            },
        });
    }

    public listEditorsForForms(formIds: number[]): Promise<FormEditor[]> {
        return this.get<FormEditor[]>('/api/form-editors/', {
            query: {
                formIds: formIds.join(','),
            },
        });
    }

    public listEditorsForForm(formId: number): Promise<FormEditor[]> {
        return this.get<FormEditor[]>(`/api/form-editors/${formId}/`);
    }

    public xdfTransform(value: string | ArrayBuffer): Promise<VFormVersionWithDetailsEntity> {
        return this.postXml<string | ArrayBuffer, VFormVersionWithDetailsEntity>('/api/xdf/v2/transform/', value);
    }

    public getLockState(id: number): Promise<EntityLockDto> {
        return this.get<EntityLockDto>(`/api/forms/${id}/lock/`);
    }

    public deleteLockState(id: number): Promise<void> {
        return this.delete(`/api/forms/${id}/lock/`);
    }

    public async getIdentityProviders(slug: string, version?: number): Promise<Page<IdentityProviderInfo>> {
        return await this.get(`/api/public/forms/${slug}/identity-providers/`, {
            query: {
                version: version,
            },
            skipAuthCheck: true,
        });
    }

    public async deriveForm(slug: string, version: number, customerInput: CustomerInput, filter: {
        skipErrorsFor: DerivationSkipIdentifier,
        skipVisibilitiesFor: DerivationSkipIdentifier,
        skipValuesFor: DerivationSkipIdentifier,
        skipOverridesFor: DerivationSkipIdentifier,
    }): Promise<ElementDerivationResponse> {
        const opt: RequestOptions = {
            query: {
                ...filter,
                version: version,
            },
        };

        return await this.post<CustomerInput, ElementDerivationResponse>(
            `/api/public/forms/${slug}/derive`,
            customerInput,
            {
                ...opt,
                skipAuthCheck: true,
            },
        );
    }

    public checkPublish({formId, version}: FormVersionEntityId): Promise<FormPublishChecklistItem[]> {
        return this.get<FormPublishChecklistItem[]>(`/api/forms/${formId}/${version}/check-publish/`);
    }

    public publish({formId, version}: FormVersionEntityId): Promise<FormVersionEntity> {
        return this.put<any, FormVersionEntity>(`/api/forms/${formId}/${version}/publish/`, {});
    }

    public revoke({formId, version}: FormVersionEntityId): Promise<FormVersionEntity> {
        return this.put<any, FormVersionEntity>(`/api/forms/${formId}/${version}/revoke/`, {});
    }

    public async getMaxFileSize(slug: string, version?: number | undefined) {
        return await this.get<{ maxFileSize: number }>(`/api/forms/${slug}/max-file-size/`, {
            query: {
                version: version,
            },
            skipAuthCheck: true,
        });
    }

    public async listRevisions(id: FormVersionEntityId, options?: ApiOptions): Promise<Page<FormRevision>> {
        return await this.get<Page<FormRevision>>(`/api/forms/${id.formId}/${id.version}/revisions/`);
    }

    public async rollbackRevision(formId: FormVersionEntityId, revisionId: number): Promise<FormVersionEntity> {
        return await this.get<FormVersionEntity>(`/api/forms/${formId.formId}/${formId.version}/revisions/rollback/${revisionId}/`);
    }

    public getFormTheme(slug: string, version?: number): Promise<Theme> {
        return this.get<Theme>(`/api/public/forms/${slug}/theme/`, {
            query: {
                version: version,
            },
            skipAuthCheck: true,
        });
    }

    public getFormLogoLink(slug: string, version?: number): string {
        let url = `/api/public/forms/${slug}/logo/`;
        if (version !== undefined) {
            url += `?version=${version}`;
        }
        return url;
    }

    public getFormFaviconLink(slug: string, version?: number): string {
        let url = `/api/public/forms/${slug}/favicon/`;
        if (version !== undefined) {
            url += `?version=${version}`;
        }
        return url;
    }

    public async calculateCosts(slug: string, version: number, customerInput: CustomerInput): Promise<FormCostCalculationResponseDTO> {
        return await this.post<AuthoredElementValues, FormCostCalculationResponseDTO>(`/api/public/forms/${slug}/costs/`, customerInput, {query: {version: version}});
    }

    public async submit(id: FormVersionEntityId, rootElement: RootElement, userInput: CustomerInput, identityId: string | undefined): Promise<SubmissionListResponseDTO> {
        const data = new FormData();
        data.set('inputs', JSON.stringify(userInput));

        const files: FileUploadElementItem[] = [];

        function processElementValues(element: any, values: AuthoredElementValues) {
            const value = values[element.id];

            if (element.type === ElementType.FileUpload && Array.isArray(value)) {
                files.push(...value);
            }

            if (isReplicatingContainerLayout(element)) {
                if (Array.isArray(value)) {
                    for (const item of value) {
                        if (item == null || typeof item !== 'object') {
                            continue;
                        }

                        for (const child of element.children ?? []) {
                            processElementValues(child, item as AuthoredElementValues);
                        }
                    }
                }

                return;
            }

            if (isAnyElementWithChildren(element)) {
                for (const child of element.children ?? []) {
                    processElementValues(child, values);
                }
            }
        }

        processElementValues(rootElement, userInput);

        for (const file of files) {
            const blob = await fetch(file.uri).then((r) => r.blob());
            data.append('files', blob, file.name);
        }

        return await this.postFormData<SubmissionListResponseDTO>(`/api/public/submit/${id.formId}/${id.version}/`, data, {
            headers: identityId != null ? {
                [IdentityIdHeader]: identityId ?? undefined,
            } : undefined,
            skipAuthCheck: true,
        });
    }

    public async sendApplicationCopy(submissionId: string, email: string): Promise<{ success: boolean; }> {
        return await this.post<{ email: string }, { success: boolean; }>(`/api/public/send-copy/${submissionId}/`, {
            email: email,
        }, {
            skipAuthCheck: true,
        });
    }

    public async rateApplication(submissionId: string, score: number): Promise<string> {
        return await this.get<string>(
            `/api/rate/${submissionId}/`, {
                query: {
                    score: score.toFixed(0),
                },
                skipAuthCheck: true,
            },
        );
    }
}
