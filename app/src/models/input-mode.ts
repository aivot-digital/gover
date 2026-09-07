import {type NoCodeOperand} from './functions/no-code-expression';
import {type ProcessNodeEntity} from '../modules/process/entities/process-node-entity';

export type InputMode = 'Literal' | 'Variable' | 'NoCode' | 'LowCode';
export type InputVariableSource = 'ProcessData' | 'ElementData' | 'ElementMetadata' | 'ProtectedProcessData';

export interface InputModePolicy {
    allowedModes: InputMode[];
    defaultMode?: InputMode | null;
    allowedVariableSources?: InputVariableSource[];
}

export interface DynamicTextPolicy {
    variableSuggestionSources: InputVariableSource[];
}

export interface InputVariableReference {
    source: InputVariableSource;
    path: string;
    nodeDataKey?: string | null;
}

export interface InputVariableSuggestion extends InputVariableReference {
    label: string;
    description?: string | null;
    origin?: ProcessNodeEntity | null;
}

export type AuthoredInputValue<T> =
    | {type: 'Literal'; value: T | null}
    | {type: 'Variable'; reference: InputVariableReference}
    | {type: 'NoCode'; operand: NoCodeOperand}
    | {type: 'LowCode'; code: string};

export function isAuthoredInputValue<T>(value: unknown): value is AuthoredInputValue<T> {
    if (value == null || typeof value !== 'object') {
        return false;
    }
    const candidate = value as Record<string, unknown>;
    return candidate.type === 'Literal' && 'value' in candidate ||
        candidate.type === 'Variable' && 'reference' in candidate ||
        candidate.type === 'NoCode' && 'operand' in candidate ||
        candidate.type === 'LowCode' && 'code' in candidate;
}

export function normalizeAuthoredInputValue<T>(value: unknown): AuthoredInputValue<T> {
    // Component demos may pass a raw initial value; persisted AuthoredElementValues are always wrapped.
    return isAuthoredInputValue<T>(value) ? value : {type: 'Literal', value: value as T | null};
}

export function getInputVariableKey(reference: InputVariableReference): string {
    return [reference.source, reference.nodeDataKey ?? '', reference.path].join(':');
}

export function getInputVariableReference(reference: InputVariableReference): string {
    switch (reference.source) {
        case 'ProcessData':
            return `$.${reference.path}`;
        case 'ElementData':
            return `_.${reference.nodeDataKey}.${reference.path}`;
        case 'ElementMetadata':
            return `$$.taskMetadata.${reference.nodeDataKey}.${reference.path}`;
        case 'ProtectedProcessData':
            return `$$.${reference.path}`;
    }
}
