import {Application} from '../models/application';
import {CodeService} from '../services/code.service';
import {downloadTextFile} from './download-text-file';

export function downloadConfigFile(application: Application): void {
    if (application != null) {
        downloadTextFile(
            `${application.slug} - ${application.version}.gov`,
            JSON.stringify(application),
            'application/json'
        );
    }
}

export function downloadCodeFile(application: Application): void {
    if (application != null) {
        CodeService.getCodeString(application.id)
            .then(code => downloadTextFile(
                `${application.slug} - ${application.version}.js`,
                code,
                'text/javascript'
            ));
    }
}
