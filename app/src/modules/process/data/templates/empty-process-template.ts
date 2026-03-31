import {ProcessExport} from '../../entities/process-export';
import {AppInfo} from '../../../../app-info';
import {ProcessStatus} from '../../enums/process-status';

export const EmptyProcessTemplate: ProcessExport = {
    appVersion: AppInfo.version,
    createdByVendor: '',
    exportTimestamp: new Date().toISOString(),
    process: {
        id: 0,
        internalTitle: 'Neues Verfahren',
        departmentId: 0,
        accessKey: '',
        versionCount: 0,
        draftedVersion: null,
        publishedVersion: null,
        created: new Date().toISOString(),
        updated: new Date().toISOString(),
    },
    version: {
        processId: 0,
        processVersion: 0,
        status: ProcessStatus.Drafted,
        publicTitle: 'Neues Verfahren',
        crated: new Date().toISOString(),
        updated: new Date().toISOString(),
        published: null,
        revoked: null,
    },
    nodes: [],
    edges: [],
};