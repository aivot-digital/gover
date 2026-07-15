export const ModuleFlag = {
    Form: 'FORM',
    ProcessUnlimited: 'PROCESS_UNLIMITED',
} as const;

export type ModuleFlag = typeof ModuleFlag[keyof typeof ModuleFlag];

export function hasModuleFlag(flag: ModuleFlag): boolean {
    return AppConfig.moduleFlags?.includes(flag) ?? false;
}

export function isFormModuleEnabled(): boolean {
    return hasModuleFlag(ModuleFlag.Form);
}

export function isProcessUnlimitedModuleEnabled(): boolean {
    return hasModuleFlag(ModuleFlag.ProcessUnlimited);
}

export function getProcessNodeLimit(type: string): number {
    const limit = AppConfig.processNodeLimits?.[type];
    return typeof limit === 'number' ? limit : -1;
}

export function isProcessNodeTypeUnlimited(type: string): boolean {
    return isProcessUnlimitedModuleEnabled() || getProcessNodeLimit(type) < 0;
}

export const ModuleFlagLabels: Record<ModuleFlag, string> = {
    [ModuleFlag.Form]: "Formulare",
    [ModuleFlag.ProcessUnlimited]: "Prozesse",
}