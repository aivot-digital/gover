import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {Form} from '../../models/entities/form';
import {ApplicationStatus} from '../../data/application-status';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {ElementType} from '../../data/element-type/element-type';
import {RootElement} from '../../models/elements/root-element';
import {ApiOptions} from '../../services/api-service';
import {Page} from '../../models/dtos/page';
import {FormRevision} from '../../models/entities/form-revision';
import {CustomerInput} from '../../models/customer-input';
import {FormState} from '../../models/dtos/form-state';
import {FormCostCalculationResponseDTO} from './dtos/form-cost-calculation-response-dto';
import {FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {EntityLockDto} from '../../models/dtos/entity-lock-dto';
import {SubmissionListResponseDTO} from '../submissions/dtos/submission-list-response-dto';
import {FormCitizenListResponseDTO} from './dtos/form-citizen-list-response-dto';
import {FormPublishChecklistItem} from './dtos/form-publish-checklist-item';
import {FormType} from './enums/form-type';
import {ElementApprovalStatus} from '../elements/enums/ElementApprovalStatus';

interface FormFilters {
    id: number;
    title: string;
    slug: string;
    version: string;
    status: string;
    type: FormType;
    destinationId: number;
    legalSupportDepartmentId: number;
    technicalSupportDepartmentId: number;
    imprintDepartmentId: number;
    privacyDepartmentId: number;
    accessibilityDepartmentId: number;
    developingDepartmentId: number;
    managingDepartmentId: number;
    responsibleDepartmentId: number;
    themeId: number;
    bundIdEnabled: boolean;
    bayernIdEnabled: boolean;
    mukEnabled: boolean;
    shIdEnabled: boolean;
    pdfBodyTemplateKey: string;
    paymentProvider: string;
    userId: string;
    isDeveloper: boolean;
    isManager: boolean;
    isResponsible: boolean;
}

export type DerivationStepIdentifiers = string[] | ['NONE'] | ['ALL'];

export class FormsApiService extends CrudApiService<Form, Form, FormCitizenListResponseDTO, Form, Form, number, FormFilters> {
    public constructor(api: Api) {
        super(api, 'forms/');
    }

    public initialize(): Form {
        return FormsApiService.initialize();
    }

    public static initialize(): Form {
        return {
            id: 0,
            slug: '',
            version: '',
            title: '',
            status: ApplicationStatus.Drafted,
            type: FormType.Public,
            root: generateElementWithDefaultValues(ElementType.Root) as RootElement,

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

            created: '',
            updated: '',

            customerAccessHours: 4,
            submissionDeletionWeeks: 4,

            bundIdEnabled: false,
            bundIdLevel: undefined,

            bayernIdEnabled: false,
            bayernIdLevel: undefined,

            shIdEnabled: false,
            shIdLevel: undefined,

            mukEnabled: false,
            mukLevel: undefined,

            pdfBodyTemplateKey: null,

            products: undefined,
            paymentPurpose: undefined,
            paymentDescription: undefined,
            paymentProvider: undefined,

            identityRequired: false,
            identityProviders: [],
        };
    }

    public async listRevisions(formId: number, options?: ApiOptions): Promise<Page<FormRevision>> {
        return await this.api.get<Page<FormRevision>>(`forms/${formId}/revisions/`, options);
    }

    // TODO: Remove usage of useFormsApi().rollbackRevision and use this instead
    public async rollbackRevision(formId: number, revisionId: number, options?: ApiOptions): Promise<Form> {
        return await this.api.get<Form>(`forms/${formId}/revisions/rollback/${revisionId}/`, options);
    }

    public async determineFormState(formId: number, customerInput: CustomerInput, filter: {
        stepsToValidate: DerivationStepIdentifiers,
        stepsToCalculateVisibilities: DerivationStepIdentifiers,
        stepsToCalculateValues: DerivationStepIdentifiers,
        stepsToCalculateOverrides: DerivationStepIdentifiers,
    }): Promise<FormState> {
        return await this.api.post<FormState>(`public/forms/${formId}/derive`, customerInput, {queryParams: filter});
    }

    public async calculateCosts(formId: number, customerInput: CustomerInput): Promise<FormCostCalculationResponseDTO> {
        return await this.api.post<FormCostCalculationResponseDTO>(`public/forms/${formId}/costs/`, customerInput);
    }

    public async determineApprovals(formId: number): Promise<Record<string, ElementApprovalStatus>> {
        return await this.api.get<Record<string, ElementApprovalStatus>>(`public/forms/${formId}/approvals/`);
    }

    public async submit(id: number, userInput: CustomerInput): Promise<SubmissionListResponseDTO> {
        const data = new FormData();
        data.set('inputs', JSON.stringify(userInput));

        const fileSets = Object
            .keys(userInput)
            .filter((key) => {
                const val = userInput[key];
                return Array.isArray(val) && val.length > 0 && val[0].uri != null;
            }).map((key) => userInput[key]) as unknown as FileUploadElementItem[][];

        for (const fileSet of fileSets) {
            for (const file of fileSet) {
                const blob = await fetch(file.uri).then((r) => r.blob());
                data.append('files', blob, file.name);
            }
        }

        return await this.api.postFormData<SubmissionListResponseDTO>(`public/submit/${id}/`, data);
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

    async getMaxFileSize(id: number) {
        return await this.api.getPublic<{ maxFileSize: number }>(`forms/${id}/max-file-size/`);
    }

    public retrieveBySlugAndVersion(slug: string, version: string | undefined) {
        if (version == null) {
            return this.api.getPublic<Form>(`forms/${slug}/`);
        }
        return this.api.getPublic<Form>(`forms/${slug}/${version}/`);
    }

    public publish(id: number): Promise<Form> {
        return this.api.put<Form>(`forms/${id}/publish/`, {});
    }

    public revoke(id: number): Promise<Form> {
        return this.api.put<Form>(`forms/${id}/revoke/`, {});
    }

    public checkPublish(id: number): Promise<FormPublishChecklistItem[]> {
        return this.api.get<FormPublishChecklistItem[]>(`forms/${id}/check-publish/`);
    }
}