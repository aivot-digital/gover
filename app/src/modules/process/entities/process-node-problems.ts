import {ProcessNodeEntity} from './process-node-entity';
import {DerivedRuntimeElementData} from '../../../models/element-data';

export interface ProcessNodeProblems {
    node: ProcessNodeEntity;
    problems: string[];
    commonErrors: Record<string, string>;
    derivedRuntimeElementData: DerivedRuntimeElementData;
}