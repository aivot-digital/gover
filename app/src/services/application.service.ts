import {RootElement} from '../models/elements/root-element';
import {ElementType} from '../data/element-type/element-type';
import {Application} from '../models/application';
import {ApiDetailsResponse} from '../models/api-details-response';
import {CrudService} from './crud.service';
import {CustomerInput} from '../models/customer-input';
import axios, {AxiosResponse} from 'axios';
import {ApiConfig} from '../api-config';
import {generateComponentPatch} from '../utils/generate-component-patch';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ApplicationStatus} from '../data/application-status/application-status';


class _ApplicationService extends CrudService<Application, 'applications', number>{
    constructor() {
        super('applications');
    }

    async createNew(slug: string, version: string, title: string, model?: RootElement): Promise<ApiDetailsResponse<Application>> {
        const newRoot: RootElement = {
            ...generateElementWithDefaultValues<ElementType.Root>(ElementType.Root) as RootElement,
            ...model,
        };
        newRoot.title = title;
        const newApplication: Omit<Application, 'id'> = {
            slug,
            version,
            root: newRoot,
            created: '',
            updated: '',
        };

        return await this.create(newApplication);
    }

    async clone(oldId: number, newSlug: string, newVersion: string, newTitle: string): Promise<ApiDetailsResponse<Application>> {
        const model = await this.retrieve(oldId);
        model.root.status = ApplicationStatus.Drafted;
        return await this.createNew(newSlug, newVersion, newTitle, model.root);
    }

    async retrieve(id: number): Promise<ApiDetailsResponse<Application>> {
        const data = await super.retrieve(id);
        _ApplicationService.normalizeAppModel(data.root);
        return data;
    }

    async retrieveBySlug(slug: string, version: string):  Promise<Application> {
        const response: AxiosResponse = await axios.get(ApiConfig.address + '/public/applications/' + slug + '/' + version, CrudService.getConfig());
        const application = response.data;
        _ApplicationService.normalizeAppModel(application.root);
        return application;
    }

    async submit(application: Application, userInput: CustomerInput): Promise<string> {
        const customerData = {...userInput};

        /*
        The generation of the patched values is from now on handled on the server.
        We keep this for the very unlikely case of shifting it back to the client.
        const extractValues = (elem: AnyElement) => {
            const patchedElem = generateComponentPatch(elem.id, elem, userInput);
            if (patchedElem != null && (patchedElem as any).value != null) {
                customerData[elem.id] = (patchedElem as any).value;
            }
            if (isAnyElementWithChildren(elem)) {
                elem.children.forEach(extractValues);
            }
        };
        extractValues(application.root);
         */

        return await axios.post(
            ApiConfig.address + '/public/submit/' + application.id,
            customerData, {
                ...CrudService.getConfig(),
                timeout: 1000 * 60 * 2 // Set 2 Minutes Timeout
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

    private static normalizeAppModel(model: RootElement): RootElement {
        /* TODO: Check this
        if (model.generalInformation == null) {
            model.generalInformation = {
                type: ElementType.GeneralInformation,
                id: generateElementId('gi'),
                ...ElementDefaults[ElementType.GeneralInformation]
            };
        }
        if (model.summary == null) {
            model.summary = {
                type: ElementType.Summary,
                id: generateElementId('ov'),
                ...ElementDefaults[ElementType.Summary]
            };
        }
        if (model.submit == null) {
            model.submit = {
                type: ElementType.Submit,
                id: generateElementId('sm'),
                ...ElementDefaults[ElementType.Submit]
            };
        }

        if (model.headline == null) {
            model.headline = 'Antrag auf Förderung aus dem \nProgramm „[NAME DES PROGRAMMS]“';
        }
        if (model.privacyText == null) {
            model.privacyText = ElementDefaults[ElementType.Root].privacyText;
        }
         */
        return model;
    }
}

export const ApplicationService = new _ApplicationService();
