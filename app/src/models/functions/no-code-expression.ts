export interface NoCodeExpression {
    operatorIdentifier: string;
    operands: (NoCodeOperand | null)[];
}

export function isNoCodeExpression(obj: any): obj is NoCodeExpression {
    return obj != null && (obj as NoCodeExpression).operatorIdentifier !== undefined;
}

export interface NoCodeReference {
    elementId: string;
}

export function isNoCodeReference(obj: any): obj is NoCodeReference {
    return obj != null && (obj as NoCodeReference).elementId !== undefined;
}

export interface NoCodeStaticValue {
    value: string;
}

export function isNoCodeStaticValue(obj: any): obj is NoCodeStaticValue {
    return obj != null && (obj as NoCodeStaticValue).value !== undefined;
}

export type NoCodeOperand = NoCodeExpression | NoCodeReference | NoCodeStaticValue;

export interface ValidationExpressionWrapper {
    expression: NoCodeExpression;
    message: string;
}