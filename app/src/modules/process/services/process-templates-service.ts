import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {AppInfo} from '../../../app-info';
import {ProcessExport} from '../entities/process-export';

const DEV = import.meta.env.DEV;
const REG = AppConfigV2.registryHostname;

export interface TemplateRegistryIndex {
    processes: TemplateRegistryProcessItem[];
    nodes: TemplateRegistryNodeItem[];
}

export interface TemplateRegistryProcessItem {
    name: string;
    description: string;
    vendor: string;
    path: string;
    appVersion: string;
    appBuildNumber: string;
}

export interface TemplateRegistryNodeItem {
    name: string;
    description: string;
    vendor: string;
    path: string;
    appVersion: string;
    appBuildNumber: string;
}

function getRegistryURL(path: string): string | null {
    if (isStringNullOrEmpty(REG)) {
        return null;
    }

    if (REG.endsWith('/')) {
        return REG + path;
    }

    return REG + '/' + path;
}

export class ProcessTemplatesService  {
    public async getProcessTemplates(): Promise<TemplateRegistryProcessItem[]> {
        const url = getRegistryURL('index.json');

        if (url == null) {
            return [];
        }

        const res = await fetch(url, {
            method: 'GET',
        });

        const registryIndex: TemplateRegistryIndex = await res.json();

        return registryIndex
            .processes
            .filter(p => DEV || p.appVersion === AppInfo.version);
    }

    public async loadTemplate(template: TemplateRegistryProcessItem): Promise<ProcessExport | null> {
        const url = getRegistryURL(template.path);

        if (url == null) {
            return Promise.resolve(null);
        }

        const res = await fetch(url, {
            method: 'GET',
        });

        return await res.json();
    }
}