import {RootElement} from '../../../models/elements/root-element';
import {IdentityProviderLink} from '../../identity/models/identity-provider-link';
import {LoadedForm} from '../../../slices/app-slice';
import {FormEntity} from '../entities/form-entity';
import {FormVersionEntity} from '../entities/form-version-entity';
import {FormType} from '../enums/form-type';
import {FormStatus} from '../enums/form-status';
import {VFormWithPermissionsEntity} from '../entities/v-form-with-permissions-entity';

export interface FormCitizenDetailsResponseDTO {
    id: number;
    slug: string;
    version: number;
    title: string;
    root: RootElement;
    legalSupportDepartmentId?: number;
    technicalSupportDepartmentId?: number;
    imprintDepartmentId?: number;
    privacyDepartmentId?: number;
    accessibilityDepartmentId?: number;
    developingDepartmentId: number;
    managingDepartmentId?: number;
    responsibleDepartmentId?: number;
    themeId: number;
    identityRequired: boolean;
    identityProviders: IdentityProviderLink[];
}


export function formCitizenDetailsResponseDTO(citizenForm: FormCitizenDetailsResponseDTO): LoadedForm {
    const form: FormEntity = {
        id: citizenForm.id,
        internalTitle: '',

        slug: citizenForm.slug,
        developingDepartmentId: citizenForm.developingDepartmentId,

        draftedVersion: 0,
        publishedVersion: 0,
        versionCount: 0,

        created: new Date().toISOString(),
        updated: new Date().toISOString(),
    };

    const version: FormVersionEntity = {
        formId: citizenForm.id,
        version: citizenForm.version,

        publicTitle: citizenForm.title,

        type: FormType.Public,
        status: FormStatus.Published,

        responsibleDepartmentId: citizenForm.responsibleDepartmentId ?? null,
        managingDepartmentId: citizenForm.managingDepartmentId ?? null,


        privacyDepartmentId: citizenForm.privacyDepartmentId ?? null,
        imprintDepartmentId: citizenForm.imprintDepartmentId ?? null,
        accessibilityDepartmentId: citizenForm.accessibilityDepartmentId ?? null,

        legalSupportDepartmentId: citizenForm.legalSupportDepartmentId ?? null,
        technicalSupportDepartmentId: citizenForm.technicalSupportDepartmentId ?? null,

        rootElement: citizenForm.root,

        customerAccessHours: null,
        submissionRetentionWeeks: null,

        identityProviders: citizenForm.identityProviders,
        identityVerificationRequired: citizenForm.identityRequired,

        paymentDescription: '',
        paymentProducts: [],
        paymentProviderKey: null,
        paymentPurpose: '',

        pdfTemplateKey: null,

        destinationId: null,
        themeId: citizenForm.themeId,

        created: new Date().toISOString(),
        updated: new Date().toISOString(),
        revoked: null,
        published: null,
    };

    const perm: VFormWithPermissionsEntity = {
        ...form,
        userId: '',
        formPermissionAnnotate: false,
        formPermissionCreate: false,
        formPermissionDelete: false,
        formPermissionEdit: false,
        formPermissionPublish: false,
        formPermissionRead: true,
    };

    return {
        form: form,
        version: version,
        permissions: perm,
    };
}