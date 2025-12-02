import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {FormStatus} from '../enums/form-status';
import {FormType} from '../enums/form-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementType} from '../../../data/element-type/element-type';
import {RootElement} from '../../../models/elements/root-element';
import {VFormVersionWithDetailsEntity} from '../entities/v-form-version-with-details-entity';

interface FormVersionFilter {
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
    status: FormStatus;
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
}

export interface FormVersionEntityId {
    formId: number;
    version: number | 'latest';
}

export class VFormVersionWithDetailsService extends BaseCrudApiService<VFormVersionWithDetailsEntity, VFormVersionWithDetailsEntity, VFormVersionWithDetailsEntity, VFormVersionWithDetailsEntity, FormVersionEntityId, FormVersionFilter> {
    public constructor() {
        super('/api/form-versions-with-details/');
    }

    public buildPath(id: FormVersionEntityId): string {
        return `${this.path}/${id.formId}/${id.version}/`;
    }

    public initialize(): VFormVersionWithDetailsEntity {
        return VFormVersionWithDetailsService.initialize();
    }

    public static initialize(): VFormVersionWithDetailsEntity {
        return {
            accessibilityDepartmentId: 0,
            created: '',
            customerAccessHours: 0,
            destinationId: 0,
            developingDepartmentId: 0,
            draftedVersion: 0,
            formId: 0,
            id: 0,
            identityProviders: [],
            identityVerificationRequired: false,
            imprintDepartmentId: 0,
            internalTitle: '',
            legalSupportDepartmentId: 0,
            managingDepartmentId: 0,
            paymentDescription: '',
            paymentProducts: [],
            paymentProviderKey: '',
            paymentPurpose: '',
            pdfTemplateKey: '',
            privacyDepartmentId: 0,
            publicTitle: '',
            published: '',
            publishedVersion: 0,
            responsibleDepartmentId: 0,
            revoked: '',
            rootElement: generateElementWithDefaultValues(ElementType.Root) as RootElement,
            slug: '',
            status: FormStatus.Drafted,
            submissionRetentionWeeks: 0,
            technicalSupportDepartmentId: 0,
            themeId: 0,
            type: FormType.Public,
            updated: '',
            version: 0,
            versionCount: 0
        }
    }
}