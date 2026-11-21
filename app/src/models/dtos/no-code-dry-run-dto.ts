import {NoCodeOperand} from '../functions/no-code-expression';
import {DerivedRuntimeElementData} from '../element-data';

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
    message: string | null;
    operands: NoCodeDryRunResultDTO[];
}
