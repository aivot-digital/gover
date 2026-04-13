import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {AppInfo} from '../../../app-info';
import {ProcessExport} from '../entities/process-export';

export interface TemplateRegistryIndex {
    processes: TemplateRegistryProcessItem[];
    nodes: TemplateRegistryNodeItem[];
}

export interface TemplateRegistryProcessItem {
    name: string;
    vendor: string;
    path: string;
    appVersion: string;
    appBuildNumber: string;
}

export interface TemplateRegistryNodeItem {
    name: string;
    vendor: string;
    path: string;
    appVersion: string;
    appBuildNumber: string;
}

function getRegistryURL(path: string): string {
    if (isStringNullOrEmpty(AppConfig.registry.url)) {
        return '';
    }

    if (AppConfig.registry.url.endsWith('/')) {
        return AppConfig.registry.url + path;
    }

    return AppConfig.registry.url + '/' + path;
}

export class ProcessTemplatesService  {
    public async getProcessTemplates(): Promise<TemplateRegistryProcessItem[]> {
        if (isStringNullOrEmpty(AppConfig.registry.url)) {
            return [];
        }

        const res = await fetch(getRegistryURL('index.json'), {
            method: 'GET',
        });

        const registryIndex: TemplateRegistryIndex = await res.json();

        return registryIndex
            .processes
            .filter(p => p.appVersion === AppInfo.version);
    }

    public async loadTemplate(template: TemplateRegistryProcessItem): Promise<ProcessExport> {
        if (isStringNullOrEmpty(AppConfig.registry.url)) {
            throw new Error('Registry URL is not configured');
        }

        const res = await fetch(getRegistryURL(template.path), {
            method: 'GET',
        });

        return await res.json();
    }
}