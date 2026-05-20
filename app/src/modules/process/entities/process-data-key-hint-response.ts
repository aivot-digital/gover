import {type ProcessNodeEntity} from './process-node-entity';

export interface ProcessDataKeyHintResponse {
    key: string;
    type: 'ProcessData' | 'ElementData';
    node: ProcessNodeEntity;
}
