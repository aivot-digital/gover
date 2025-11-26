import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {Page} from '../../models/dtos/page';
import {FormCitizenListResponseDTO} from './dtos/form-citizen-list-response-dto';
import {FormType} from './enums/form-type';
import {FormListResponseDTO} from './dtos/form-list-response-dto';
import {FormDetailsResponseDTO} from './dtos/form-details-response-dto';
import {FormRequestDTO} from './dtos/form-request-dto';
import {RootElement} from '../../models/elements/root-element';
import {FormStatus} from './enums/form-status';
import {Theme} from '../themes/models/theme';
import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {FormEditor} from './dtos/form-editor';
import {IdentityIdHeader} from '../identity/constants/identity-id-header';
import {RequestOptions} from '../../services/base-api-service';
import {type FormCitizenDetailsResponseDTO} from './dtos/form-citizen-details-response-dto';
import {CustomerInput} from '../../models/customer-input';
import {ElementData, ElementDerivationResponse} from '../../models/element-data';
import {DerivationSkipIdentifier} from './forms-api-service';
import {SubmissionListResponseDTO} from '../submissions/dtos/submission-list-response-dto';
import {FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';

interface FormFilters {
    id: number;
    slug: string;
    internalTitle: string;
    publicTitle: string;
    developingDepartmentId: number;
    developingDepartmentIdNot: number;
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

export type FormIdentifier = {
    id: number;
    version: number;
}

export class FormsApiService extends BaseCrudApiService<FormRequestDTO, FormListResponseDTO, FormDetailsResponseDTO, FormRequestDTO, FormIdentifier, FormFilters, 'formInternalTitle' | 'formUpdated'> {
    constructor() {
        super('/api/forms/');
    }

    public initialize(): FormDetailsResponseDTO {
        return FormsApiService
            .initialize();
    }

    public buildPath(id: FormIdentifier): string {
        return `${this.path}${id.id}/${id.version}/`;
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

    public async retrieveLatest(formId: number): Promise<FormDetailsResponseDTO> {
        return this.get<FormDetailsResponseDTO>(`/api/forms/${formId}/latest/`);
    }

    public async moveFormToDepartment(formId: number, departmentId: number): Promise<void> {
        this.put<any, void>(`/api/forms/${formId}/move/?targetDepartmentId=${departmentId}`, {});
    }

    public getFormTheme(formSlug: string, formVersion?: number | string): Promise<Theme> {
        return this.get<Theme>(`/api/public/forms/${formSlug}/theme/`, {
            query: {
                version: formVersion,
            },
            skipAuthCheck: true,
        });
    }

    public getFormLogoLink(formSlug: string, formVersion?: number): string {
        return this.createPath(`/api/public/forms/${formSlug}/logo/`, {
            version: formVersion,
        });
    }

    public getFormFaviconLink(formSlug: string, formVersion?: number): string {
        return this.createPath(`/api/public/forms/${formSlug}/favicon/`, {
            version: formVersion,
        });
    }

    public listPublicAll(): Promise<FormCitizenListResponseDTO[]> {
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

    public async determineFormState(slug: string, version: number, customerInput: CustomerInput, filter: {
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

        return await this.postFormData<SubmissionListResponseDTO>(`/api/public/submit/${id.id}/${id.version}/`, data, {
            headers: identityId != null ? {
                [IdentityIdHeader]: identityId ?? undefined,
            } : undefined,
            skipAuthCheck: true,
        });
    }

    public async destroyAll(formId: number): Promise<void> {
        return await this.delete(`/api/forms/${formId}/`);
    }
}
