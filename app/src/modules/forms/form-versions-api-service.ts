import {FormType} from './enums/form-type';
import {FormDetailsResponseDTO} from './dtos/form-details-response-dto';
import {FormRequestDTO} from './dtos/form-request-dto';
import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {FormsApiService} from './forms-api-service-v2';

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

export class FormVersionApiService extends BaseCrudApiService<FormDetailsResponseDTO, FormDetailsResponseDTO, FormDetailsResponseDTO, FormRequestDTO, number, FormFilters> {
    constructor(formId: number) {
        super(`/api/forms/${formId}/versions/`);
    }

    public initialize(): FormDetailsResponseDTO {
        return FormsApiService.initialize();
    }
}