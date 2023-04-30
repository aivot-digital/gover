export interface ConditionOperandReference {
    id: string;
}

export function isConditionOperandReference(obj: any): obj is ConditionOperandReference {
    return obj != null && 'id' in obj;
}
