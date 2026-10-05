import {type NoCodeOperand} from './functions/no-code-expression';
import {type ProcessNodeEntity} from '../modules/process/entities/process-node-entity';

// These string values are part of the backend JSON contract, not display labels.
export enum InputMode {
    Literal = 'Literal',
    Variable = 'Variable',
    NoCode = 'NoCode',
    LowCode = 'LowCode',
}

export enum InputVariableSource {
    ProcessData = 'ProcessData',
    ElementData = 'ElementData',
    ElementMetadata = 'ElementMetadata',
    ProtectedProcessData = 'ProtectedProcessData',
}

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
    | {type: InputMode.Literal; value: T | null}
    | {type: InputMode.Variable; reference: InputVariableReference}
    | {type: InputMode.NoCode; operand: NoCodeOperand}
    | {type: InputMode.LowCode; code: string};

export function isAuthoredInputValue<T>(value: unknown): value is AuthoredInputValue<T> {
    if (value == null || typeof value !== 'object') {
        return false;
    }
    const candidate = value as Record<string, unknown>;
    return candidate.type === InputMode.Literal && 'value' in candidate ||
        candidate.type === InputMode.Variable && 'reference' in candidate ||
        candidate.type === InputMode.NoCode && 'operand' in candidate ||
        candidate.type === InputMode.LowCode && 'code' in candidate;
}

export function normalizeAuthoredInputValue<T>(value: unknown): AuthoredInputValue<T> {
    // Component demos may pass a raw initial value; persisted AuthoredElementValues are always wrapped.
    return isAuthoredInputValue<T>(value) ? value : {type: InputMode.Literal, value: value as T | null};
}

export function getInputVariableKey(reference: InputVariableReference): string {
    return [reference.source, reference.nodeDataKey ?? '', reference.path].join(':');
}

export function getInputVariableReference(reference: InputVariableReference): string {
    switch (reference.source) {
        case InputVariableSource.ProcessData:
            return `$.${reference.path}`;
        case InputVariableSource.ElementData:
            return `_.${reference.nodeDataKey}${reference.path.startsWith('[') ? '' : '.'}${reference.path}`;
        case InputVariableSource.ElementMetadata:
            return `$$.taskMetadata.${reference.nodeDataKey}${reference.path.startsWith('[') ? '' : '.'}${reference.path}`;
        case InputVariableSource.ProtectedProcessData:
            return `$$.${reference.path}`;
    }
}
