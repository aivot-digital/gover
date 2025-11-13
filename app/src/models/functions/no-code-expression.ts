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

export interface NoCodeStaticValue {
    type: 'NoCodeStaticValue';
    value: string | null | undefined;
}

export function isNoCodeStaticValue(obj: any): obj is NoCodeStaticValue {
    return obj != null && obj.type === 'NoCodeStaticValue';
}

export type NoCodeOperand = NoCodeExpression | NoCodeReference | NoCodeStaticValue;

export interface ValidationExpressionWrapper {
    noCode: NoCodeOperand | null | undefined;
    message: string | null | undefined;
}