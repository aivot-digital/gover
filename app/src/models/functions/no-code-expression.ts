export interface NoCodeExpression {
    type: 'NoCodeExpression';
    operatorIdentifier: string | null | undefined;
    operands: (NoCodeOperand | null)[] | null | undefined;
}

export function isNoCodeExpression(obj: any): obj is NoCodeExpression {
    return obj != null && obj.type === 'NoCodeExpression';
}

export interface NoCodeReference {
    type: 'NoCodeReference';
    elementId: string | null | undefined;
}

export function isNoCodeReference(obj: any): obj is NoCodeReference {
    return obj != null && obj.type === 'NoCodeReference';
}

export interface NoCodeProcessDataReference {
    type: 'NoCodeProcessDataReference';
    path: string | null | undefined;
}

export function isNoCodeProcessDataReference(obj: any): obj is NoCodeProcessDataReference {
    return obj != null && obj.type === 'NoCodeProcessDataReference';
}

export interface NoCodeInstanceDataReference {
    type: 'NoCodeInstanceDataReference';
    path: string | null | undefined;
}

export function isNoCodeInstanceDataReference(obj: any): obj is NoCodeInstanceDataReference {
    return obj != null && obj.type === 'NoCodeInstanceDataReference';
}

export interface NoCodeNodeDataReference {
    type: 'NoCodeNodeDataReference';
    nodeDataKey: string | null | undefined;
    path: string | null | undefined;
}

export function isNoCodeNodeDataReference(obj: any): obj is NoCodeNodeDataReference {
    return obj != null && obj.type === 'NoCodeNodeDataReference';
}

export interface NoCodeStaticValue {
    type: 'NoCodeStaticValue';
    value: string | null | undefined;
}

export function isNoCodeStaticValue(obj: any): obj is NoCodeStaticValue {
    return obj != null && obj.type === 'NoCodeStaticValue';
}

export type NoCodeOperand =
    | NoCodeExpression
    | NoCodeReference
    | NoCodeProcessDataReference
    | NoCodeInstanceDataReference
    | NoCodeNodeDataReference
    | NoCodeStaticValue;

export interface ValidationExpressionWrapper {
    noCode: NoCodeOperand | null | undefined;
    message: string | null | undefined;
}
