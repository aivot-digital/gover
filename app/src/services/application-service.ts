import {type Application} from '../models/entities/application';
import {type CustomerInput} from '../models/customer-input';
import {type FileUploadElementItem} from '../models/elements/form/input/file-upload-element';
import {ApiService} from './api-service';
import {type ListApplication} from '../models/entities/list-application';
import {type PublicListApplication} from '../models/entities/public-list-application';
import {type SubmissionListDto} from '../models/entities/submission-list-dto';


class _ApplicationService extends ApiService<Application, ListApplication, number> {
    constructor() {
        super('applications');
    }

    public async listPublic(): Promise<PublicListApplication[]> {
        return await ApiService.get('public/applications');
    }

    public async retrievePublic(slug: string, version: string): Promise<Application> {
        return await ApiService.get(`public/applications/${slug}/${version}`);
    }

    async submit(application: Application, userInput: CustomerInput): Promise<SubmissionListDto> {
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

        return await ApiService.postFormData(`public/submit/${application.id}`, data);
    }

    async sendApplicationCopy(submission: SubmissionListDto, email: string): Promise<string> {
        return await ApiService.post(
            `public/send-copy/${submission.id}`,
            {
                email,
            },
        );
    }

    async rateApplication(submission: SubmissionListDto, score: number): Promise<string> {
        return await ApiService.get(
            `public/rate/${submission.id}?score=${score.toFixed(0)}`,
        );
    }

    async getMaxFileSize(application: Application): Promise<{maxFileSize: number}> {
        return await ApiService.get(`public/max-file-size/${application.id}`);
    }
}

export const ApplicationService = new _ApplicationService();
