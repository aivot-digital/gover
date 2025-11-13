export interface ThemeRequestDTO {
    name: string;
    main: string;
    mainDark: string;
    accent: string;
    error: string;
    warning: string;
    info: string;
    success: string;
    faviconKey: string | null;
    logoKey: string | null;
}

export interface ThemeResponseDTO extends ThemeRequestDTO {
    id: number;
}

export type Theme = ThemeResponseDTO;