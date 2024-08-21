import {type Form as Application} from '../models/entities/form';
import {stripDataFromForm} from './strip-data-from-form';

export function downloadTextFile(filename: string, content: string, mimetype: string): void {
    const file = new Blob([content], {type: mimetype});
    downloadBlobFile(filename, file);
}

export function downloadBlobFile(filename: string, blob: Blob): void {
    const a = document.createElement('a');
    const url = URL.createObjectURL(blob);
    a.href = url;
    a.download = filename;
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}

export function downloadObjectFile(filename: string, obj: any): void {
    downloadTextFile(filename, JSON.stringify(obj), 'application/json');
}

export function downloadConfigFile(application?: Application): void {
    if (application != null) {
        const app = stripDataFromForm(application);
        downloadObjectFile(`${app.slug} - ${app.version}.gov`, app);
    }
}

export function uploadTextFile(accepts: string): Promise<string | null> {
    return new Promise((resolve, reject) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = accepts;
        input.addEventListener('change', (event) => {
            if (input.files != null) {
                const reader = new FileReader();
                reader.onload = (evt) => {
                    const value = evt.target?.result;
                    if (value != null) {
                        resolve(value.toString());
                    } else {
                        reject(new Error('Empty reader result'));
                    }
                };
                reader.onerror = reject;
                reader.readAsText(input.files[0], 'UTF-8');
            }
            input.remove();
        });
        input.click();
    });
}

export function uploadObjectFile<T>(accepts: string): Promise<T | null> {
    return uploadTextFile(accepts)
        .then((res) => res != null ? JSON.parse(res) : null);
}
