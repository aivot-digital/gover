import * as React from 'react';
import {type SvgIconComponent} from '../../types/svg-icon-component';
import {Box, type SvgIconProps, type SxProps, type Theme} from '@mui/material';

export type IconBadgeCorner = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
type IconBadgeSource = SvgIconComponent | React.ReactElement<SvgIconProps>;

export interface IconBadgeProps extends Omit<React.ComponentPropsWithoutRef<typeof Box>, 'children'> {
    icon: IconBadgeSource;
    badgeIcon: IconBadgeSource;
    corner?: IconBadgeCorner;
    size?: number | string;
    badgeSize?: number | string;
    badgeInset?: number | string;
    cutoutSize?: number | string;
    sx?: SxProps<Theme>;
    iconProps?: SvgIconProps;
    badgeIconProps?: SvgIconProps;
    iconContainerSx?: SxProps<Theme>;
    badgeContainerSx?: SxProps<Theme>;
}

const badgeCornerStyles: Record<IconBadgeCorner, SxProps<Theme>> = {
    'top-left': {
        top: 0,
        left: 0,
        transform: 'translate(-50%, -50%)',
    },
    'top-right': {
        top: 0,
        right: 0,
        transform: 'translate(50%, -50%)',
    },
    'bottom-left': {
        bottom: 0,
        left: 0,
        transform: 'translate(-50%, 50%)',
    },
    'bottom-right': {
        right: 0,
        bottom: 0,
        transform: 'translate(50%, 50%)',
    },
};

const iconCutoutMaskPositions: Record<IconBadgeCorner, string> = {
    'top-left': 'var(--icon-badge-inset) var(--icon-badge-inset)',
    'top-right': 'calc(100% - var(--icon-badge-inset)) var(--icon-badge-inset)',
    'bottom-left': 'var(--icon-badge-inset) calc(100% - var(--icon-badge-inset))',
    'bottom-right': 'calc(100% - var(--icon-badge-inset)) calc(100% - var(--icon-badge-inset))',
};

function mergeSx(...values: Array<SxProps<Theme> | undefined>): SxProps<Theme> | undefined {
    const merged = values.flatMap((value) => {
        if (value == null) {
            return [];
        }

        return Array.isArray(value) ? value : [value];
    });

    return merged.length > 0 ? merged : undefined;
}

function getBadgeInsetStyle(corner: IconBadgeCorner, badgeInset: number | string): SxProps<Theme> {
    return {
        top: corner.startsWith('top') ? badgeInset : undefined,
        right: corner.endsWith('right') ? badgeInset : undefined,
        bottom: corner.startsWith('bottom') ? badgeInset : undefined,
        left: corner.endsWith('left') ? badgeInset : undefined,
    };
}

function toCssLength(value: number | string): string {
    return typeof value === 'number' ? `${value}px` : value;
}

function getIconCutoutMaskStyle(corner: IconBadgeCorner): SxProps<Theme> {
    const maskImage = `radial-gradient(circle at ${iconCutoutMaskPositions[corner]}, transparent var(--icon-badge-cutout-size), #000 var(--icon-badge-cutout-size))`;

    return {
        WebkitMask: `${maskImage} center / 100% 100% no-repeat`,
        mask: `${maskImage} center / 100% 100% no-repeat`,
    };
}

function renderIcon(icon: IconBadgeSource, props?: SvgIconProps) {
    if (React.isValidElement<SvgIconProps>(icon)) {
        return React.cloneElement(icon, {
            ...icon.props,
            ...props,
            sx: mergeSx(
                {fontSize: 'inherit'},
                icon.props.sx,
                props?.sx,
            ),
        });
    }

    const Icon = icon;

    return (
        <Icon
            {...props}
            sx={mergeSx({fontSize: 'inherit'}, props?.sx)}
        />
    );
}

export function IconBadge(props: IconBadgeProps): React.ReactElement {
    const {
        icon,
        badgeIcon,
        corner = 'bottom-right',
        size = 24,
        badgeSize = 14,
        badgeInset = 0,
        cutoutSize,
        sx,
        iconProps,
        badgeIconProps,
        iconContainerSx,
        badgeContainerSx,
        ...rest
    } = props;

    const resolvedBadgeInset = toCssLength(badgeInset);
    const resolvedCutoutSize = cutoutSize == null
        ? `calc((${toCssLength(badgeSize)} * 0.7) + ${resolvedBadgeInset})`
        : toCssLength(cutoutSize);

    return (
        <Box
            component="span"
            sx={mergeSx({
                '--icon-badge-inset': resolvedBadgeInset,
                '--icon-badge-cutout-size': resolvedCutoutSize,
                '--icon-badge-diameter': 'calc(var(--icon-badge-cutout-size) * 2)',
                position: 'relative',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: size,
                height: size,
                fontSize: size,
                lineHeight: 0,
                flexShrink: 0,
                verticalAlign: 'middle',
            }, sx)}
            {...rest}
        >
            <Box
                component="span"
                sx={mergeSx({
                    width: '100%',
                    height: '100%',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                }, getIconCutoutMaskStyle(corner), iconContainerSx)}
            >
                {renderIcon(icon, iconProps)}
            </Box>

            <Box
                component="span"
                sx={mergeSx({
                    position: 'absolute',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 'var(--icon-badge-diameter)',
                    height: 'var(--icon-badge-diameter)',
                    boxSizing: 'border-box',
                    fontSize: badgeSize,
                    lineHeight: 0,
                    borderRadius: '50%',
                    //backgroundColor: 'background.paper',
                    boxShadow: (theme) => `0 0 0 1px ${theme.palette.background.paper}`,
                    pointerEvents: 'none',
                }, badgeCornerStyles[corner], getBadgeInsetStyle(corner, 'var(--icon-badge-inset)'), badgeContainerSx)}
            >
                {renderIcon(badgeIcon, badgeIconProps)}
            </Box>
        </Box>
    );
}
