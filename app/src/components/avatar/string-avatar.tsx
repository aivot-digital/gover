import * as React from 'react';
import MuiAvatar, { type AvatarProps as MuiAvatarProps } from '@mui/material/Avatar';
import type { SxProps, Theme } from '@mui/material/styles';

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

export type StringAvatarProps = Omit<MuiAvatarProps, 'children'> & {
    name: string;
};

export function StringAvatar(props: StringAvatarProps) {
    const { name, sx, ...rest } = props;

    const safeName = (name ?? '').trim() || 'Unbekannte Nutzer:in';
    const bg = React.useMemo(() => stringToPastelColor(safeName), [safeName]);
    const initials = React.useMemo(() => getInitials(safeName), [safeName]);

    const baseSx: SxProps<Theme> = React.useMemo(
        () => (theme) => ({
            bgcolor: bg,
            color: theme.palette.getContrastText(bg),
            fontWeight: 700,
        }),
        [bg]
    );

    return (
        <MuiAvatar
            {...rest}
            sx={mergeSx(baseSx, sx)}
            aria-label={`Avatar für ${safeName}`}
            title={safeName}
        >
            {initials}
        </MuiAvatar>
    );
}