import {ProcessExportData} from "../../entities/process-export";
import {AppInfo} from "../../../../app-info";
import {ProcessStatus} from "../../enums/process-status";
import {RetentionTimeUnit} from "../../enums/retention-time-unit";
import {ProcessNodeApiService} from "../../services/process-node-api-service";
import {ProcessDefinitionApiService} from "../../services/process-definition-api-service";
import {ProcessDefinitionVersionApiService} from "../../services/process-definition-version-api-service";

export const ApiProcessTemplate: ProcessExportData = {
    appVersion: AppInfo.version,
    createdByVendor: '',
    exportTimestamp: new Date().toISOString(),
    process: {
        ...ProcessDefinitionApiService.initialize(),
        name: "API-gesteuertes Verfahren",
    },
    version: {
        ...ProcessDefinitionVersionApiService.initialize(),
        status: ProcessStatus.Drafted,
    },
    nodes: [
        {
            ...ProcessNodeApiService.initialize(),
            processNodeDefinitionKey: 'webhook',
            processNodeDefinitionVersion: 1,
        },
    ],
    edges: [],
}