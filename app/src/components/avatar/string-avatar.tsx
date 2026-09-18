import * as React from 'react';
import MuiAvatar, { type AvatarProps as MuiAvatarProps } from '@mui/material/Avatar';
import { alpha, darken, lighten, type SxProps, type Theme } from '@mui/material/styles';

function hashString(input: string): number {
    let hash = 0;
    for (let i = 0; i < input.length; i++) {
        hash = input.charCodeAt(i) + ((hash << 5) - hash);
        hash |= 0;
    }
    return Math.abs(hash);
}

export function stringToPastelColor(input: string): string {
    const hash = hashString(input);
    const hue = hash % 360;
    const saturation = 45 + (hash % 16); // 45–60
    const lightness = 78 + (hash % 10); // 78–87
    return `hsl(${hue} ${saturation}% ${lightness}%)`;
}

type MarblePalette = readonly [string, string, string, string, string];
export type StringAvatarBackgroundMode = 'preset' | 'theme' | 'pastel' | 'oklch';
export const DEFAULT_STRING_AVATAR_BACKGROUND_MODE: StringAvatarBackgroundMode = 'preset';

const MARBLE_PALETTES: readonly MarblePalette[] = [
    ['#0d0514', '#4d0382', '#f7005f', '#f57f15', '#f3b640'],
    ['#d8dfc9', '#99c7ab', '#69b1a3', '#26203a', '#954761'],
    ['#604456', '#b95e62', '#d58379', '#f3cca6', '#9fbda9'],
    ['#e4ebcb', '#c7cec0', '#99929f', '#554665', '#32183b'],
    ['#71966d', '#f7db90', '#f88336', '#c75106', '#760c0c'],
    ['#281c15', '#d0a573', '#e7cb94', '#bfc591', '#acbf9a'],
    ['#1e0b23', '#512151', '#0a6b72', '#4da17f', '#e8bf84'],
    ['#d5d09f', '#d7605b', '#c93245', '#623b50', '#4d646d'],
    ['#e06b2d', '#be505d', '#533f40', '#aa9a76', '#ccba8b'],
    ['#aebfb3', '#e3cdae', '#e2b79b', '#c58a77', '#8e6b66'],
];

function createThemeMarblePalette(theme: Theme): MarblePalette {
    const primaryMain = theme.palette.primary.main;
    const primaryDark = theme.palette.primary.dark;
    const secondaryMain = theme.palette.secondary.main;

    return [
        primaryMain,
        primaryDark,
        secondaryMain,
        darken(primaryDark, 0.16),
        lighten(secondaryMain, 0.1),
    ];
}

function normalizeHue(hue: number): number {
    return (hue % 360 + 360) % 360;
}

function createOklchPastelPalette(input: string): MarblePalette {
    const hash = hashString(input);
    const baseHue = hash % 360;
    const hueStep = 36 + (hash % 17); // 24–40° broader analogous spread
    const lightness = 84; // pastel
    const chroma = 0.08; // pastel, slightly more vivid

    return [
        `oklch(${lightness}% ${chroma} ${normalizeHue(baseHue - (hueStep * 2))})`,
        `oklch(${lightness}% ${chroma} ${normalizeHue(baseHue - hueStep)})`,
        `oklch(${lightness}% ${chroma} ${normalizeHue(baseHue)})`,
        `oklch(${lightness}% ${chroma} ${normalizeHue(baseHue + hueStep)})`,
        `oklch(${lightness}% ${chroma} ${normalizeHue(baseHue + (hueStep * 2))})`,
    ];
}

type MarbleBlob = {
    colorIndex: number;
    x: number;
    y: number;
    size: number;
    opacity: number;
};

type MarbleSpec = {
    paletteIndex: number;
    baseColorIndex: number;
    tiltDeg: number;
    blobs: MarbleBlob[];
};

function seededUnit(seed: number, salt: number): number {
    const value = Math.sin(seed * 12.9898 + salt * 78.233) * 43758.5453123;
    return value - Math.floor(value);
}

function withAlpha(color: string, opacity: number): string {
    if (color.startsWith('oklch(') && color.endsWith(')')) {
        const normalizedOpacity = Math.max(0, Math.min(1, opacity));
        const colorBody = color.slice('oklch('.length, -1).trim();
        return `oklch(${colorBody} / ${normalizedOpacity})`;
    }

    return alpha(color, opacity);
}

function createMarbleSpec(input: string): MarbleSpec {
    const hash = hashString(input);
    const blobs: MarbleBlob[] = Array.from({ length: 5 }, (_, index) => ({
        colorIndex: Math.floor(seededUnit(hash, index + 1) * 5) % 5,
        x: 10 + seededUnit(hash, index + 11) * 80,
        y: 10 + seededUnit(hash, index + 21) * 80,
        size: 34 + seededUnit(hash, index + 31) * 30,
        opacity: 0.34 + seededUnit(hash, index + 41) * 0.34,
    }));

    return {
        paletteIndex: hash % MARBLE_PALETTES.length,
        baseColorIndex: Math.floor(seededUnit(hash, 77) * 5) % 5,
        tiltDeg: Math.round(seededUnit(hash, 99) * 360),
        blobs,
    };
}

export function getInitials(fullName: string): string {
    const parts = fullName
        .trim()
        .split(/\s+/)
        .filter(Boolean);

    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();

    const first = parts[0][0] ?? '';
    const last = parts[parts.length - 1][0] ?? '';
    return `${first}${last}`.toUpperCase();
}

function mergeSx(base: SxProps<Theme>, extra?: SxProps<Theme>): SxProps<Theme> {
    if (!extra) return base;

    const baseArr = Array.isArray(base) ? base : [base];
    const extraArr = Array.isArray(extra) ? extra : [extra];

    return [...baseArr, ...extraArr];
}

export type StringAvatarProps = MuiAvatarProps & {
    name: string;
    backgroundMode?: StringAvatarBackgroundMode;
    showInitials?: boolean;
};

export function StringAvatar(props: StringAvatarProps) {
    const { name, sx, backgroundMode = DEFAULT_STRING_AVATAR_BACKGROUND_MODE, showInitials = true, children, ...rest } = props;

    const safeName = (name ?? '').trim() || 'Unbekannte Nutzer:in';
    const marbleSpec = React.useMemo(() => createMarbleSpec(safeName), [safeName]);
    const pastelColor = React.useMemo(() => stringToPastelColor(safeName), [safeName]);
    const oklchPalette = React.useMemo(() => createOklchPastelPalette(safeName), [safeName]);
    const initials = React.useMemo(() => getInitials(safeName), [safeName]);

    const baseSx: SxProps<Theme> = React.useMemo(
        () => (theme) => {
            if (backgroundMode === 'pastel') {
                return {
                    bgcolor: pastelColor,
                    color: theme.palette.getContrastText(pastelColor),
                    fontWeight: 700,
                    overflow: 'hidden',
                };
            }

            const marblePalette = backgroundMode === 'theme'
                ? createThemeMarblePalette(theme)
                : backgroundMode === 'oklch'
                    ? oklchPalette
                    : (MARBLE_PALETTES[marbleSpec.paletteIndex] ?? MARBLE_PALETTES[0]);
            const baseColor = marblePalette[marbleSpec.baseColorIndex] ?? marblePalette[0];
            const useDarkText = backgroundMode === 'oklch';
            const linearOverlayOpacity = useDarkText ? 0.3 : 0.18;
            const radialOverlayOpacity = useDarkText ? 0.34 : 0.2;
            const blobGradients = marbleSpec.blobs
                .map((blob) => {
                    const color = marblePalette[blob.colorIndex] ?? marblePalette[0];
                    return `radial-gradient(circle at ${blob.x}% ${blob.y}%, ${withAlpha(color, blob.opacity)} 0%, ${withAlpha(color, blob.opacity * 0.8)} ${blob.size * 0.58}%, ${withAlpha(color, 0)} ${blob.size}%)`;
                })
                .join(', ');

            return {
                backgroundColor: withAlpha(baseColor, useDarkText ? 0.34 : 0.16),
                backgroundImage: [
                    `linear-gradient(${marbleSpec.tiltDeg}deg, ${withAlpha(marblePalette[1], linearOverlayOpacity)} 0%, ${withAlpha(marblePalette[3], 0)} 58%)`,
                    `radial-gradient(circle at 24% 22%, ${withAlpha(marblePalette[4], radialOverlayOpacity)} 0%, ${withAlpha(marblePalette[4], 0)} 42%)`,
                    blobGradients,
                ].join(', '),
                backgroundSize: '140% 140%',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
                color: useDarkText ? theme.palette.text.primary : '#ffffff',
                fontWeight: 700,
                textShadow: useDarkText ? 'none' : `0 2px 3px ${alpha('#000000', 0.55)}, 0 0 8px ${alpha('#000000', 0.35)}`,
                overflow: 'hidden',
            };
        },
        [backgroundMode, marbleSpec, oklchPalette, pastelColor]
    );

    return (
        <MuiAvatar
            {...rest}
            sx={mergeSx(baseSx, sx)}
            aria-label={`Avatar für ${safeName}`}
            title={safeName}
        >
            {children ?? (showInitials ? initials : '\u00A0')}
        </MuiAvatar>
    );
}
