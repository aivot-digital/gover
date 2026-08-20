import React, {useId, useState} from 'react';
import {
    Button,
    IconButton,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Menu,
    MenuItem,
    Tooltip,
    type SvgIconProps,
    type TooltipProps,
} from '@mui/material';
import Check from '@aivot/mui-material-symbols-400-n25-outlined/Check';
import DarkMode from '@aivot/mui-material-symbols-400-n25-outlined/DarkMode';
import Contrast from '@aivot/mui-material-symbols-400-n25-outlined/Contrast';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import LightMode from '@aivot/mui-material-symbols-400-n25-outlined/LightMode';
import {type ColorModePreference, useColorMode} from '../../providers/color-mode-context';

const colorModeOptions: Array<{
    value: ColorModePreference;
    label: string;
    shortLabel: string;
}> = [
    {value: 'system', label: 'System-Standard', shortLabel: 'System'},
    {value: 'light', label: 'Hell', shortLabel: 'Hell'},
    {value: 'dark', label: 'Dunkel', shortLabel: 'Dunkel'},
];

type ColorModePickerPlacement = 'top-end' | 'right-end' | 'bottom-end';

interface ColorModePickerProps {
    showLabel?: boolean;
    placement?: ColorModePickerPlacement;
    tooltipPlacement?: TooltipProps['placement'];
    color?: 'inherit' | 'primary';
    iconFontSize?: SvgIconProps['fontSize'];
    size?: 'small' | 'medium' | 'large';
}

function ColorModeIcon({preference, fontSize}: {
    preference: ColorModePreference;
    fontSize?: SvgIconProps['fontSize'];
}) {
    switch (preference) {
        case 'light':
            return <LightMode fontSize={fontSize}/>;
        case 'dark':
            return <DarkMode fontSize={fontSize}/>;
        default:
            return <Contrast fontSize={fontSize}/>;
    }
}

export function ColorModePicker({
    showLabel = false,
    placement = 'bottom-end',
    tooltipPlacement = 'bottom',
    color = 'inherit',
    iconFontSize = showLabel ? 'small' : 'medium',
    size = 'medium',
}: ColorModePickerProps) {
    const {mode, preference, setPreference} = useColorMode();
    const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
    const componentId = useId().replaceAll(':', '');
    const buttonId = `color-mode-menu-button-${componentId}`;
    const menuId = `color-mode-menu-${componentId}`;
    const selectedOption = colorModeOptions.find((option) => option.value === preference) ?? colorModeOptions[0];
    const resolvedModeLabel = mode === 'dark' ? 'Dunkel' : 'Hell';
    const selectedIndicatorLabel = preference === 'system'
        ? `System (${resolvedModeLabel})`
        : selectedOption.shortLabel;
    const accessibleLabel = preference === 'system'
        ? `Darstellung: System-Standard, aktuell ${resolvedModeLabel.toLowerCase()}`
        : `Darstellung: ${selectedOption.label}`;
    const open = anchorEl != null;
    const ariaProps = {
        id: buttonId,
        'aria-label': accessibleLabel,
        'aria-controls': open ? menuId : undefined,
        'aria-haspopup': 'menu' as const,
        'aria-expanded': open ? 'true' as const : undefined,
    };
    const menuPosition = placement === 'right-end'
        ? {
            anchorOrigin: {vertical: 'bottom' as const, horizontal: 'right' as const},
            transformOrigin: {vertical: 'bottom' as const, horizontal: 'left' as const},
        }
        : placement === 'top-end'
            ? {
                anchorOrigin: {vertical: 'top' as const, horizontal: 'right' as const},
                transformOrigin: {vertical: 'bottom' as const, horizontal: 'right' as const},
            }
            : {
                anchorOrigin: {vertical: 'bottom' as const, horizontal: 'right' as const},
                transformOrigin: {vertical: 'top' as const, horizontal: 'right' as const},
            };

    const control = showLabel ? (
        <Button
            {...ariaProps}
            size={size}
            color={color}
            onClick={(event) => setAnchorEl(event.currentTarget)}
            startIcon={<ColorModeIcon preference={preference} fontSize={iconFontSize}/>}
            endIcon={<KeyboardArrowDown/>}
            sx={{
                minWidth: 0,
                px: 0.75,
                color: color === 'inherit' ? 'text.secondary' : undefined,
                '& .MuiButton-startIcon': {ml: 0, mr: 0.5},
                '& .MuiButton-endIcon': {ml: 0.25, mr: 0},
                '&:hover': {
                    color: color === 'inherit' ? 'text.primary' : undefined,
                    backgroundColor: 'action.hover',
                },
            }}
        >
            {selectedIndicatorLabel}
        </Button>
    ) : (
        <IconButton
            {...ariaProps}
            color={color}
            size={size}
            onClick={(event) => setAnchorEl(event.currentTarget)}
        >
            <ColorModeIcon preference={preference} fontSize={iconFontSize}/>
        </IconButton>
    );

    return (
        <>
            {showLabel ? control : (
                <Tooltip title={accessibleLabel} placement={tooltipPlacement} arrow>
                    {control}
                </Tooltip>
            )}
            <Menu
                id={menuId}
                anchorEl={anchorEl}
                open={open}
                onClose={() => setAnchorEl(null)}
                anchorOrigin={menuPosition.anchorOrigin}
                transformOrigin={menuPosition.transformOrigin}
                slotProps={{list: {'aria-labelledby': buttonId}}}
            >
                <ListSubheader
                    disableSticky
                    sx={{
                        pb: 0.75,
                        lineHeight: 1.5,
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        letterSpacing: 0,
                        color: 'text.secondary',
                        backgroundColor: 'transparent',
                    }}
                >
                    Darstellung
                </ListSubheader>
                {colorModeOptions.map((option) => (
                    <MenuItem
                        key={option.value}
                        selected={option.value === preference}
                        onClick={() => {
                            setPreference(option.value);
                            setAnchorEl(null);
                        }}
                    >
                        <ListItemIcon>
                            <ColorModeIcon preference={option.value} fontSize="small"/>
                        </ListItemIcon>
                        <ListItemText>{option.label}</ListItemText>
                        {option.value === preference && <Check fontSize="small" sx={{ml: 1.5}}/>}
                    </MenuItem>
                ))}
            </Menu>
        </>
    );
}
