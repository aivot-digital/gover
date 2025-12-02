import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {FormVersionEntity} from '../entities/form-version-entity';
import {FormStatus} from '../enums/form-status';
import {FormType} from '../enums/form-type';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {RootElement} from '../../../models/elements/root-element';
import {ElementType} from '../../../data/element-type/element-type';
import {FormEditor} from '../dtos/form-editor';
import {ApiError} from '../../../models/api-error';

interface FormVersionFilter {
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

const DraftExistsError: ApiError = {
    status: 400,
    message: 'Es existiert bereits ein Entwurf für dieses Formular. Bitte veröffentlichen oder löschen Sie den Entwurf, bevor Sie eine neue Version erstellen.',
    details: null,
    displayableToUser: true,
};

export class FormVersionApiService extends BaseCrudApiService<FormVersionEntity, FormVersionEntity, FormVersionEntity, FormVersionEntity, FormVersionEntityId, FormVersionFilter> {
    public constructor() {
        super('/api/form-versions/');
    }

    public buildPath(id: FormVersionEntityId): string {
        return `${this.path}${id.formId}/${id.version}/`;
    }

    public initialize(): FormVersionEntity {
        return FormVersionApiService.initialize();
    }

    public static initialize(): FormVersionEntity {
        return {
            accessibilityDepartmentId: 0,
            created: '',
            customerAccessHours: 0,
            destinationId: 0,
            formId: 0,
            identityProviders: [],
            identityVerificationRequired: false,
            imprintDepartmentId: 0,
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
            responsibleDepartmentId: 0,
            revoked: '',
            rootElement: generateElementWithDefaultValues(ElementType.Root) as RootElement,
            status: FormStatus.Drafted,
            submissionRetentionWeeks: 0,
            technicalSupportDepartmentId: 0,
            themeId: 0,
            type: FormType.Public,
            updated: '',
            version: 0

        }
    }

    public listEditorsForForms(formIds: number[]): Promise<FormEditor[]> {
        return this.get<FormEditor[]>('/api/form-editors/', {
            query: {
                formIds: formIds.join(','),
            },
        });
    }

    public async latestAsNewVersion(formId: number): Promise<FormVersionEntity> {
        const latestVersion = await this.retrieve({
            formId: formId,
            version: 'latest',
        });

        if (latestVersion.status === FormStatus.Drafted) {
            throw DraftExistsError;
        }

        return this.create({
            ...latestVersion,
        });
    }

    public async versionAsNewVersion({formId, version}: FormVersionEntityId): Promise<FormVersionEntity> {
        const specifiedVersion = await this.retrieve({
            formId: formId,
            version: version,
        });

        if (specifiedVersion.status === FormStatus.Drafted) {
            throw DraftExistsError;
        }

        return this.create({
            ...specifiedVersion,
        });
    }
}