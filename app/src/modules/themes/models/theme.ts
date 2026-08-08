export interface ThemeRequestDTO {
    name: string;
    primaryColor: string;
    secondaryColor: string;
    primaryColorDark: string | null;
    secondaryColorDark: string | null;
    faviconKey: string | null;
    logoKey: string | null;
}

export interface ThemeResponseDTO extends ThemeRequestDTO {
    id: number;
}

export type Theme = ThemeResponseDTO;
