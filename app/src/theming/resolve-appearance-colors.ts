import type {ThemeRequestDTO} from '../modules/themes/models/theme';

export const MINIMUM_THEME_CONTRAST = 4.5;

export const DEFAULT_APPEARANCE_COLORS = {
    primaryColor: '#733635',
    secondaryColor: '#A0C9CB',
    primaryColorDark: '#FF613A',
    secondaryColorDark: '#A0C9CB',
} as const;

export interface ResolvedAppearanceColors {
    primary: string;
    onPrimary: string;
    primaryForeground: string;
    secondary: string;
    onSecondary: string;
    secondaryForeground: string;
}

type AppearanceColorInput = Pick<ThemeRequestDTO, 'primaryColor' | 'secondaryColor'>;

const DEFAULT_PAPER_COLOR = '#FFFFFF';

export function resolveAppearanceColors(
    input?: Partial<AppearanceColorInput>,
    foregroundBackground = DEFAULT_PAPER_COLOR,
): ResolvedAppearanceColors {
    const primary = normalizeHex(input?.primaryColor ?? DEFAULT_APPEARANCE_COLORS.primaryColor);
    const secondary = normalizeHex(input?.secondaryColor ?? DEFAULT_APPEARANCE_COLORS.secondaryColor);
    const normalizedForegroundBackground = normalizeHex(foregroundBackground);

    return {
        primary,
        onPrimary: selectContrastingText(primary),
        primaryForeground: resolveAccessibleForeground(primary, normalizedForegroundBackground),
        secondary,
        onSecondary: selectContrastingText(secondary),
        secondaryForeground: resolveAccessibleForeground(secondary, normalizedForegroundBackground),
    };
}

export function getColorContrastRatio(first: string, second: string): number {
    const firstLuminance = getRelativeLuminance(normalizeHex(first));
    const secondLuminance = getRelativeLuminance(normalizeHex(second));
    const lighter = Math.max(firstLuminance, secondLuminance);
    const darker = Math.min(firstLuminance, secondLuminance);
    return (lighter + 0.05) / (darker + 0.05);
}

export function resolveAccessibleForeground(
    candidate: string,
    background: string,
    minimumContrast = MINIMUM_THEME_CONTRAST,
): string {
    const normalizedCandidate = normalizeHex(candidate);
    const normalizedBackground = normalizeHex(background);
    if (getColorContrastRatio(normalizedCandidate, normalizedBackground) >= minimumContrast) {
        return normalizedCandidate;
    }

    const blackContrast = getColorContrastRatio('#000000', normalizedBackground);
    const whiteContrast = getColorContrastRatio('#FFFFFF', normalizedBackground);
    const target = blackContrast >= whiteContrast ? '#000000' : '#FFFFFF';

    for (let percentage = 1; percentage <= 100; percentage += 1) {
        const mixed = mixHexColors(normalizedCandidate, target, percentage / 100);
        if (getColorContrastRatio(mixed, normalizedBackground) >= minimumContrast) {
            return mixed;
        }
    }

    return target;
}

function selectContrastingText(background: string): string {
    return getColorContrastRatio('#000000', background) >= getColorContrastRatio('#FFFFFF', background)
        ? '#000000'
        : '#FFFFFF';
}

function normalizeHex(color: string): string {
    const match = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(color);
    if (match == null) {
        throw new Error(`Invalid theme color: ${color}`);
    }
    const value = match[1].length === 3
        ? match[1].split('').map((character) => character.repeat(2)).join('')
        : match[1];
    return `#${value.toUpperCase()}`;
}

function getRelativeLuminance(color: string): number {
    const [red, green, blue] = hexToRgb(color)
        .map((channel) => channel / 255)
        .map((channel) => channel <= 0.04045
            ? channel / 12.92
            : Math.pow((channel + 0.055) / 1.055, 2.4));

    return red * 0.2126 + green * 0.7152 + blue * 0.0722;
}

export function mixHexColors(first: string, second: string, secondWeight: number): string {
    if (secondWeight < 0 || secondWeight > 1) {
        throw new Error(`Invalid color weight: ${secondWeight}`);
    }

    const firstRgb = hexToRgb(normalizeHex(first));
    const secondRgb = hexToRgb(normalizeHex(second));
    const mixed = firstRgb.map((channel, index) => Math.round(
        channel * (1 - secondWeight) + secondRgb[index] * secondWeight,
    ));

    return rgbToHex(mixed[0], mixed[1], mixed[2]);
}

function hexToRgb(color: string): [number, number, number] {
    return [
        parseInt(color.slice(1, 3), 16),
        parseInt(color.slice(3, 5), 16),
        parseInt(color.slice(5, 7), 16),
    ];
}

function rgbToHex(red: number, green: number, blue: number): string {
    return `#${[red, green, blue]
        .map((channel) => channel.toString(16).padStart(2, '0'))
        .join('')}`.toUpperCase();
}
