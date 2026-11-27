import {NoCodeOperand} from '../functions/no-code-expression';
import {DerivedRuntimeElementData} from '../element-data';
import {NoCodeDataType} from '../../data/no-code-data-type';

export enum NoCodeDryRunStatus {
    Evaluated = 'Evaluated',
    Failed = 'Failed',
    Skipped = 'Skipped',
}

export interface NoCodeDryRunRequestDTO {
    expression: NoCodeOperand | null;
    elementData: DerivedRuntimeElementData;
    processData: Record<string, unknown> | null;
    instanceData: Record<string, unknown> | null;
    nodeData: Record<string, unknown> | null;
}

/** Result of one operand; the results of an expression's operands follow its operand order. */
export interface NoCodeDryRunResultDTO {
    status: NoCodeDryRunStatus;
    value: unknown;
    /** Supplied for temporal values whose type would otherwise be lost in JSON. */
    valueType?: NoCodeDataType;
    message: string | null;
    operands: NoCodeDryRunResultDTO[];
}
