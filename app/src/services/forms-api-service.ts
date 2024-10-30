import {Form} from '../models/entities/form';
import {FormRevision} from '../models/entities/form-revision';
import {Page} from '../models/dtos/page';
import {Api} from '../hooks/use-api';
import {ApiOptions} from './api-service';

export class FormsApiService {
    private readonly api: Api;

    constructor(api: Api) {
        this.api = api;
    }

    public async listRevisions(formId: number, options?: ApiOptions): Promise<Page<FormRevision>> {
        return await this.api.get<Page<FormRevision>>(`forms/${formId}/revisions`, options);
    }

    public async rollbackRevision(formId: number, revisionId: number, options?: ApiOptions): Promise<Form> {
        return await this.api.get<Form>(`forms/${formId}/revisions/rollback/${revisionId}`, options);
    }
}
