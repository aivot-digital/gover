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
import {BaseApiService} from '../../services/base-api-service';
import {Theme} from '../themes/models/theme';
import {createApiPath} from '../../utils/url-path-utils';

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

export class FormsApiService extends BaseApiService {
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

    public getFormTheme(formSlug: string, formVersion?: number): Promise<Theme> {
        return this.getUnauthenticated<Theme>(`/api/public/forms/${formSlug}/theme/`, {
            query: {
                version: formVersion,
            },
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
            .getUnauthenticated<Page<FormCitizenListResponseDTO>>('/api/public/forms/', {
                query: {
                    page: 0,
                    size: 999,
                    sort: 'publicTitle,asc',
                },
            })
            .then(({content}) => content);
    }
}