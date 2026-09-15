export interface SystemConfigDefinition {
    key: string;
    category: string;
    subCategory: string;
    label: string;
    description: string;
    isPublicConfig: boolean;
    defaultValue: string | null | undefined;
    options: string[] | null | undefined;
    type: 'FLAG' | 'TEXT' | 'ASSET' | 'SECRET' | 'DEPARTMENT' | 'THEME';
}