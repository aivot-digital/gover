import type {ThemeRequestDTO} from '../modules/themes/models/theme';

/**
 * Contrast target for brand colors used as text or similarly thin foreground elements.
 * This is a derivation target, not an accessibility guarantee for every themed component and state.
 */
export const MINIMUM_THEME_CONTRAST = 4.5;

/** Fallback brand-color inputs for the light and dark color modes. */
export const DEFAULT_APPEARANCE_COLORS = {
    primaryColor: '#733635',
    secondaryColor: '#A0C9CB',
    primaryColorDark: '#FF613A',
    secondaryColorDark: '#A0C9CB',
} as const;

/**
 * Mode-specific color roles derived from the two configured brand colors.
 *
 * The resolver itself is mode-agnostic. Its caller selects the configured light or dark colors and supplies the
 * effective Paper background for that mode. Configured main colors are retained; only dedicated foreground roles
 * may be adjusted to meet the contrast target against Paper.
 */
export interface ResolvedAppearanceColors {
    /**
     * Configured primary brand color, normalized to six-digit uppercase HEX but otherwise unchanged.
     * Used as `palette.primary.main`, especially for filled primary controls. MUI derives its additional tonal
     * variants from this value.
     */
    primary: string;

    /**
     * Black or white, whichever contrasts more strongly with `primary`.
     * Used as `palette.primary.contrastText` for text and icons placed on a solid primary-colored fill.
     */
    onPrimary: string;

    /**
     * Primary brand color for use as a foreground on Paper. It remains identical to `primary` when the configured
     * color already reaches the contrast target. Otherwise it is progressively mixed towards black or white.
     * Used for links, text and outlined primary buttons, selected tabs, tab indicators, and active step labels.
     */
    primaryForeground: string;

    /**
     * Configured secondary brand color, normalized to six-digit uppercase HEX but otherwise unchanged.
     * Used as `palette.secondary.main`, especially for filled secondary controls. MUI derives its additional tonal
     * variants from this value.
     */
    secondary: string;

    /**
     * Black or white, whichever contrasts more strongly with `secondary`.
     * Used as `palette.secondary.contrastText` for text and icons placed on a solid secondary-colored fill.
     */
    onSecondary: string;

    /**
     * Secondary brand color for use as a foreground on Paper. It remains identical to `secondary` when possible and
     * is otherwise mixed towards black or white until it reaches the contrast target.
     * Used for text and outlined secondary buttons and their borders and hover states.
     */
    secondaryForeground: string;
}

type AppearanceColorInput = Pick<ThemeRequestDTO, 'primaryColor' | 'secondaryColor'>;

const DEFAULT_PAPER_COLOR = '#FFFFFF';

/**
 * Resolves configured brand colors into fill, on-fill, and on-Paper roles.
 *
 * Missing inputs fall back to the standard light appearance. For dark mode, callers must pass the selected dark-mode
 * colors as `input` and the effective dark `palette.background.paper` as `foregroundBackground`.
 */
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

/** Calculates the WCAG contrast ratio of two opaque HEX colors. */
export function getColorContrastRatio(first: string, second: string): number {
    const firstLuminance = getRelativeLuminance(normalizeHex(first));
    const secondLuminance = getRelativeLuminance(normalizeHex(second));
    const lighter = Math.max(firstLuminance, secondLuminance);
    const darker = Math.min(firstLuminance, secondLuminance);
    return (lighter + 0.05) / (darker + 0.05);
}

/**
 * Keeps a configured foreground unchanged when it reaches the requested contrast against `background`.
 * Otherwise, it is mixed in one-percent steps towards whichever of black or white has the stronger final contrast;
 * the first color reaching the target is returned. This preserves as much of the configured brand color as possible.
 */
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

/** Selects the black or white content color with the stronger contrast against a solid fill. */
function selectContrastingText(background: string): string {
    return getColorContrastRatio('#000000', background) >= getColorContrastRatio('#FFFFFF', background)
        ? '#000000'
        : '#FFFFFF';
}

/** Expands three-digit HEX colors and normalizes all valid values to uppercase six-digit HEX. */
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

/**
 * Linearly mixes two opaque HEX colors in the RGB color space.
 * A `secondWeight` of `0` returns `first`; a value of `1` returns `second`.
 */
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
