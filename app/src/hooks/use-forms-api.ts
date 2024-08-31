import {Api} from './use-api';
import {Form, FormListProjection, FormListProjectionPublic, FormPublicProjection} from '../models/entities/form';
import {CustomerInput} from "../models/customer-input";
import type {FileUploadElementItem} from "../models/elements/form/input/file-upload-element";
import {SubmissionListProjection} from '../models/entities/submission';
import {EntityLockDto} from '../models/dtos/entity-lock-dto';
import {XBezahldienstePaymentRequest} from '../models/xbezahldienste/x-bezahldienste-payment-request';
import {FormRevision} from '../models/entities/form-revision';

interface FormsApi {
    list(filter?: {
        department?: number,
        managing?: number,
        responsible?: number,
        destination?: number,
        theme?: number,
        ids?: number[],
    }): Promise<FormListProjection[]>;

    listPublic(): Promise<FormListProjectionPublic[]>;

    listRevisions(id: number): Promise<FormRevision[]>;

    retrieve(id: number): Promise<Form>;

    retrieveBySlugAndVersion(slug: string, version?: string): Promise<FormPublicProjection>;

    save(application: Form): Promise<Form>;

    destroy(id: number): Promise<void>;

    submit(id: number, userInput: CustomerInput): Promise<SubmissionListProjection>;

    getLockState(id: number): Promise<EntityLockDto>;

    deleteLockState(id: number): Promise<void>;

    getMaxFileSize(id: number): Promise<{ maxFileSize: number }>;

    calculateCosts(id: number, customerInput: CustomerInput): Promise<XBezahldienstePaymentRequest>;

    sendApplicationCopy(submission: SubmissionListProjection, email: string): Promise<string>;

    rateApplication(submission: SubmissionListProjection, score: number): Promise<string>;
}

export function useFormsApi(api: Api): FormsApi {
    const list = async (filter?: {
        department?: number,
        managing?: number,
        responsible?: number,
        destination?: number,
        theme?: number,
        ids?: number[],
    }) => {
        return await api.get<FormListProjection[]>('forms', {
            department: filter?.department,
            managing: filter?.managing,
            responsible: filter?.responsible,
            destination: filter?.destination,
            theme: filter?.theme,
            id: filter?.ids,
        });
    };

    const listPublic = async () => {
        return await api.getPublic<FormListProjection[]>('forms');
    };

    const listRevisions = async  (id: number) => {
        return await api.get<FormRevision[]>(`forms/${id}/revisions`);
    };

    const retrieve = async (id: number) => {
        return await api.get<Form>(`forms/${id}`);
    };

    const retrieveBySlugAndVersion = async (slug: string, version?: string) => {
        if (version != null) {
            return await api.getPublic<FormPublicProjection>(`forms/${slug}/${version}`);
        }
        return await api.getPublic<FormPublicProjection>(`forms/${slug}`);
    };

    const save = async (application: Form) => {
        if (application.id === 0) {
            return await api.post<Form>('forms', application);
        } else {
            return await api.put<Form>(`forms/${application.id}`, application);
        }
    };

    const destroy = async (id: number) => {
        return await api.destroy(`forms/${id}`);
    };

    const submit = async (id: number, userInput: CustomerInput) => {
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

        return await api.postFormData<SubmissionListProjection>(`public/submit/${id}`, data);
    };

    const getLockState = async (id: number) => {
        return await api.get<EntityLockDto>(`forms/${id}/lock`);
    };

    const deleteLockState = async (id: number) => {
        return await api.destroy<void>(`forms/${id}/lock`);
    };

    const getMaxFileSize = async (id: number) => {
        return await api.getPublic<{ maxFileSize: number }>(`forms/${id}/max-file-size`);
    };


    const calculateCosts = async (id: number, customerInput: CustomerInput) => {
        return await api.post<XBezahldienstePaymentRequest>(`public/forms/${id}/costs`, customerInput);
    };

    const sendApplicationCopy = async (submission: SubmissionListProjection, email: string) => {
        return await api.post<string>(
            `public/send-copy/${submission.id}`,
            {
                email,
            },
        );
    }

    const rateApplication = async (submission: SubmissionListProjection, score: number) => {
        return await api.getPublic<string>(
            `rate/${submission.id}` ,{score: score.toFixed(0)},
        );
    }

    return {
        list,
        listPublic,
        listRevisions,
        retrieve,
        retrieveBySlugAndVersion,
        save,
        destroy,
        submit,
        getLockState,
        deleteLockState,
        getMaxFileSize,
        calculateCosts,
        sendApplicationCopy,
        rateApplication,
    };
}
