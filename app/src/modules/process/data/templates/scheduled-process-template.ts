import {ProcessExportData} from "../../entities/process-export";
import {AppInfo} from "../../../../app-info";
import {ProcessStatus} from "../../enums/process-status";
import {RetentionTimeUnit} from "../../enums/retention-time-unit";

export const ScheduledProcessTemplate: ProcessExportData = {
    appVersion: AppInfo.version,
    createdByVendor: '',
    exportTimestamp: new Date().toISOString(),
    process: {
        id: 0,
        name: "Periodisches Verfahren",
        departmentId: 0,
        versionCount: 0,
        draftedVersion: null,
        publishedVersion: null,
        created: new Date().toISOString(),
        updated: new Date().toISOString(),
    },
    version: {
        processDefinitionId: 0,
        processDefinitionVersion: 0,
        status: ProcessStatus.Drafted,
        retentionTimeUnit: RetentionTimeUnit.Days,
        retentionTimeAmount: 30,
        crated: new Date().toISOString(),
        updated: new Date().toISOString(),
        published: null,
        revoked: null,
    },
    nodes: [],
    edges: [],
}