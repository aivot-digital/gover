import {ProcessNodeWrite} from './process-node-entity';
import {DerivedRuntimeElementData} from '../../../models/element-data';

export interface ProcessNodeProblems {
    node: ProcessNodeWrite;
    problems: string[];
    commonErrors: Record<string, string[]>;
    derivedRuntimeElementData: DerivedRuntimeElementData;
}
