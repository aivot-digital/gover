import {Application} from "../models/entities/application";
import {CodeService} from "../services/code.service";

export function downloadTextFile(filename: string, content: string, mimetype: string): void {
    const a = document.createElement('a');
    const file = new Blob([content], {type: mimetype});
    a.href = URL.createObjectURL(file);
    a.download = filename;
    a.click();
    a.remove();
}

export function downloadObjectFile(filename: string, obj: any): void {
    downloadTextFile(filename, JSON.stringify(obj), 'application/json');
}

export function downloadConfigFile(application?: Application): void {
    if (application != null) {
        downloadObjectFile(
            `${application.slug} - ${application.version}.gov`,
            application,
        );
    }
}

export function downloadCodeFile(application?: Application): void {
    if (application != null) {
        CodeService.getCodeString(application.id)
            .then(code => downloadTextFile(
                `${application.slug} - ${application.version}.js`,
                code,
                'text/javascript'
            ));
    }
}
