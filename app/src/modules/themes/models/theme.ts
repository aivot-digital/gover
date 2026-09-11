export interface ThemeRequestDTO {
    name: string;
    primaryColor: string;
    secondaryColor: string;
    primaryColorDark: string | null;
    secondaryColorDark: string | null;
    faviconKey: string | null;
    logoKey: string | null;
    logoKeyDark: string | null;
}

export interface ThemeResponseDTO extends ThemeRequestDTO {
    id: number;
}

export type Theme = ThemeResponseDTO;

export type ThemeColors = Pick<
    ThemeRequestDTO,
    'primaryColor' | 'secondaryColor' | 'primaryColorDark' | 'secondaryColorDark'
>;

export interface ResolvedThemeDTO extends ThemeColors {
    logoUrl: string;
    logoUrlDark: string;
    faviconUrl: string;
}
