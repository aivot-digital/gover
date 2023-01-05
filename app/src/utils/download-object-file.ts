import {downloadTextFile} from './download-text-file';

export function downloadObjectFile(filename: string, obj: any): void {
    downloadTextFile(filename, JSON.stringify(obj), 'application/json');
}
