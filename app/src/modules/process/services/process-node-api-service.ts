import {type RequestOptions} from '../../../services/base-api-service';
import {type ProcessNodePortResolution} from './process-node-provider-api-service';
import {BaseCrudApiService} from '../../../services/base-crud-api-service';
import {type ProcessNodeEntity, type ProcessNodeWrite} from '../entities/process-node-entity';
import {type ProcessNodeExport} from '../entities/process-node-export';
import {type GroupLayout} from '../../../models/elements/form/layout/group-layout';
import {generateProcessNodeDataKey} from '../utils/process-node-data-key';
import {isApiError} from '../../../models/api-error';
import {ProcessNodeProblems} from '../entities/process-node-problems';
import {type ProcessNodeDefinitionMetadata} from '../entities/process-node-definition-metadata';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../../../models/element-data';
import {type ElementDerivationOptions} from '../../elements/elements-api-service';

interface ProcessDefinitionNodeFilter {
    id: number;
    processId: number;
    processVersion: number;
    dataKey: string;
    processNodeDefinitionKey: string;
    processNodeDefinitionVersion: number;
}

export class ProcessNodeApiService extends BaseCrudApiService<
    ProcessNodeWrite,
    ProcessNodeEntity,
    ProcessNodeEntity,
    ProcessNodeWrite,
    number,
    ProcessDefinitionNodeFilter
> {
    constructor() {
        super('/api/process-nodes/');
    }

    public initialize(): ProcessNodeEntity {
        return ProcessNodeApiService.initialize();
    }

    public static initialize(): ProcessNodeEntity {
        return {
            ports: [],
            portResolutionProblems: [],
            notes: null,
            outputMappings: {},
            requirements: null,
            deadlines: {},
            deadlineDefinitions: [],
            id: 0,
            processId: 0,
            processVersion: 0,
            processNodeDefinitionKey: '',
            processNodeDefinitionVersion: 0,
            name: null,
            description: null,
            dataKey: generateProcessNodeDataKey(),
            configuration: {},
            savedWithErrors: false,
            created: '',
            updated: '',
        };
    }

    public async createWithGeneratedDataKey(node: ProcessNodeWrite): Promise<ProcessNodeEntity> {
        for (let attempt = 0; ; attempt++) {
            try {
                return await this.create({
                    ...node,
                    dataKey: generateProcessNodeDataKey(),
                });
            } catch (error) {
                if (
                    attempt >= 4 ||
                    !isApiError(error) ||
                    error.status !== 409 ||
                    error.details?.reason !== 'process_node_data_key_conflict'
                )
                    throw error;
            }
        }
    }

    public override create(node: ProcessNodeWrite): Promise<ProcessNodeEntity> {
        return super.create(toProcessNodeWrite(node));
    }

    public override update(id: number, node: ProcessNodeWrite, options?: RequestOptions): Promise<ProcessNodeEntity> {
        return super.update(id, toProcessNodeWrite(node), options);
    }

    public resolvePorts(node: ProcessNodeWrite, options: {forImport?: boolean; useInitialConfiguration?: boolean} = {}): Promise<ProcessNodePortResolution> {
        return this.post(`${this.path}resolve-ports/`, {node: toProcessNodeWrite(node), ...options});
    }

    public getConfigurationLayout(id: number): Promise<GroupLayout> {
        return this.get(`${this.path}${id}/configuration/`);
    }

    public getIncomingMetadata(id: number): Promise<ProcessNodeDefinitionMetadata> {
        return this.get(`${this.path}${id}/incoming-metadata/`);
    }

    public deriveConfiguration(
        id: number,
        authoredElementValues: AuthoredElementValues,
        derivationOptions: ElementDerivationOptions,
    ): Promise<DerivedRuntimeElementData> {
        return this.post(`${this.path}${id}/derive-configuration/`, {
            authoredElementValues,
            derivationOptions,
        });
    }

    public async getTesting(id: number): Promise<GroupLayout | null> {
        const res = await this.fetch('GET', `${this.path}${id}/testing/`);
        const data = await res.text();

        if (res.ok && data.length === 0) {
            return null;
        }

        return JSON.parse(data) as GroupLayout;
    }

    public export(id: number): Promise<ProcessNodeExport> {
        return this.get(`${this.path}${id}/export/`);
    }

    public import(processId: number, processVersion: number, nodeData: ProcessNodeExport): Promise<ProcessNodeEntity> {
        return this.post(`/api/process-nodes/import/${processId}/${processVersion}/`, nodeData);
    }

    public async validate(id: number): Promise<ProcessNodeProblems | null> {
        try {
            return await this.get<ProcessNodeProblems>(`${this.path}${id}/problems/`);
        } catch (err) {
            return null;
        }
    }
}

export function toProcessNodeWrite(node: ProcessNodeWrite): ProcessNodeWrite {
    const {ports: _ports, portResolutionProblems: _problems, deadlineDefinitions: _deadlines, ...authored} = node as ProcessNodeEntity;
    return authored;
}
