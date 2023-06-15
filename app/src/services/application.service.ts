import {RootElement} from '../models/elements/root-element';
import {ElementType} from '../data/element-type/element-type';
import {Application} from '../models/entities/application';
import {ApiDetailsResponse} from '../models/api-details-response';
import {CrudService} from './crud.service';
import {CustomerInput} from '../models/customer-input';
import axios, {AxiosResponse} from 'axios';
import {ApiConfig} from '../api-config';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ApplicationStatus} from '../data/application-status/application-status';
import {FileUploadElementItem} from "../models/elements/form/input/file-upload-element";
import {ListApplication} from "../models/entities/list-application";


class _ApplicationService extends CrudService<Application, 'applications', number> {
    constructor() {
        super('applications');
    }

    async createNew(slug: string, version: string, title: string, rootElement?: RootElement): Promise<ApiDetailsResponse<Application>> {
        const newRoot: RootElement = {
            ...generateElementWithDefaultValues<ElementType.Root>(ElementType.Root) as RootElement,
            ...rootElement,
        };
        newRoot.title = title;
        const newApplication: Omit<Application, 'id'> = {
            slug,
            version,
            root: newRoot,
            created: '',
            updated: '',
            status: ApplicationStatus.Drafted,
        };

        return await this.create(newApplication);
    }

    async clone(oldId: number, newSlug: string, newVersion: string, newTitle: string): Promise<ApiDetailsResponse<Application>> {
        const application = await this.retrieve(oldId);
        return await this.createNew(newSlug, newVersion, newTitle, application.root);
    }

    async listPublishedApplications(): Promise<ListApplication[]> {
        const response: AxiosResponse = await axios.get(ApiConfig.address + '/public/applications', CrudService.getConfig());
        return response.data;
    }

    async retrieveBySlug(slug: string, version: string): Promise<Application> {
        const response: AxiosResponse = await axios.get(ApiConfig.address + '/public/applications/' + slug + '/' + version, CrudService.getConfig());
        return response.data;
    }

    async submit(application: Application, userInput: CustomerInput): Promise<string> {
        const data = new FormData();
        data.set('inputs', JSON.stringify(userInput));

        const fileSets = Object
            .keys(userInput)
            .filter(key => {
                const val = userInput[key];
                return Array.isArray(val) && val.length > 0 && val[0].uri != null;
            }).map(key => userInput[key]) as unknown as FileUploadElementItem[][];

        for (const fileSet of fileSets) {
            for (const file of fileSet) {
                const blob = await fetch(file.uri).then(r => r.blob());
                data.append('files', blob, file.name);
            }
        }

        return await axios.post(
            ApiConfig.address + '/public/submit/' + application.id,
            data, {
                ...CrudService.getConfig(),
                timeout: 1000 * 60 * 5 // Set 5 Minutes Timeout
            }
        )
            .then(response => response.data);
    }

    async sendApplicationCopy(application: Application, pdfLink: string, email: string): Promise<string> {
        return await axios.post(
            ApiConfig.address + '/public/send-copy/' + application.id,
            {
                email,
                pdfLink,
            }, {
                ...CrudService.getConfig(),
                timeout: 1000 * 60 * 2 // Set 2 Minutes Timeout
            }
        )
            .then(response => response.data);
    }

    async getMaxFileSize(application: Application): Promise<number> {
        return await axios.get(
            ApiConfig.address + '/public/max-file-size/' + application.id, {
                ...CrudService.getConfig(),
                timeout: 1000 * 60 * 2 // Set 2 Minutes Timeout
            }
        )
            .then(response => response.data);
    }
}

export const ApplicationService = new _ApplicationService();
