export enum CustomLinkType {
    Dashboard = 'Dashboard',
}

export interface CustomLink {
    id: number;
    label: string;
    description: string | null;
    url: string;
    icon: string | null;
    type: CustomLinkType;
    position: number;
    enabled: boolean;
    created: string;
    updated: string;
}

export interface CustomLinkRequest {
    label: string;
    description: string | null;
    url: string;
    icon: string | null;
    type: CustomLinkType;
    enabled: boolean;
}
