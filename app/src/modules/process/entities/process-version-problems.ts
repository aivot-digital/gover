import {ProcessNodeProblems} from './process-node-problems';

export interface ProcessVersionProblems {
    versionProblems: string[];
    nodeProblems: ProcessNodeProblems[];
}
