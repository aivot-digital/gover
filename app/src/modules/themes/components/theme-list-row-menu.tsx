import {ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import StarOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Star';
import StarFilled from '@aivot/mui-material-symbols-400-n25-outlined/StarFilled';
import type {Theme} from '../models/theme';

interface ThemeListRowMenuProps {
    anchorEl: HTMLElement;
    theme: Theme;
    isDefaultTheme: boolean;
    canSetDefaultTheme: boolean;
    isSettingDefaultTheme: boolean;
    setDefaultThemeDisabledTooltip?: string;
    onClose: () => void;
    onSetDefaultTheme: (theme: Theme) => void;
}

export function ThemeListRowMenu(props: ThemeListRowMenuProps) {
    const {
        anchorEl,
        theme,
        isDefaultTheme,
        canSetDefaultTheme,
        isSettingDefaultTheme,
        setDefaultThemeDisabledTooltip,
        onClose,
        onSetDefaultTheme,
    } = props;
    const isSetDefaultDisabled = isDefaultTheme || !canSetDefaultTheme || isSettingDefaultTheme;
    const disabledReason = isDefaultTheme
        ? 'Bereits ausgewählt'
        : !canSetDefaultTheme
            ? setDefaultThemeDisabledTooltip
            : isSettingDefaultTheme
                ? 'Ein Standard-Erscheinungsbild wird bereits festgelegt.'
                : undefined;

    return (
        <Menu
            anchorEl={anchorEl}
            open
            onClose={onClose}
        >
            <MenuItem
                disabled={isSetDefaultDisabled}
                onClick={() => {
                    onClose();
                    onSetDefaultTheme(theme);
                }}
            >
                <ListItemIcon>
                    {isDefaultTheme ? <StarFilled /> : <StarOutlined />}
                </ListItemIcon>
                <ListItemText
                    primary="Als Standard festlegen"
                    secondary={disabledReason}
                />
            </MenuItem>
        </Menu>
    );
}
