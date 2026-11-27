import {useId, useState} from 'react';
import {ListItemIcon, ListItemText, Menu, MenuItem} from '@mui/material';
import FilterAlt from '@aivot/mui-material-symbols-400-n25-outlined/FilterAlt';
import CheckBox from '@aivot/mui-material-symbols-400-n25-outlined/CheckBox';
import CheckBoxOutlineBlank from '@aivot/mui-material-symbols-400-n25-outlined/CheckBoxOutlineBlank';
import {IconButton} from '../../../components/icon-button/icon-button';

export function ProcessListFilterMenu({tasks = false, includeTests, onChange}: {
    tasks?: boolean;
    includeTests: boolean;
    onChange: (includeTests: boolean) => void;
}) {
    const id = useId();
    const [anchor, setAnchor] = useState<HTMLElement | null>(null);
    const open = anchor != null;
    const tooltip = includeTests
        ? 'Weitere Filter'
        : tasks ? 'Weitere Filter: Testaufgaben ausgeblendet' : 'Weitere Filter: Testvorgänge ausgeblendet';

    return (
        <>
            <IconButton
                buttonProps={{
                    id,
                    'aria-label': tooltip,
                    'aria-haspopup': 'menu',
                    'aria-controls': open ? `${id}-menu` : undefined,
                    'aria-expanded': open,
                    color: includeTests ? 'default' : 'primary',
                    onClick: (event) => setAnchor(event.currentTarget),
                }}
                tooltipProps={{title: tooltip}}
                badgeProps={{variant: 'dot', color: 'primary', invisible: includeTests}}
            >
                <FilterAlt />
            </IconButton>
            <Menu
                anchorEl={anchor}
                open={open}
                onClose={() => setAnchor(null)}
                slotProps={{list: {id: `${id}-menu`, 'aria-labelledby': id}}}
            >
                <MenuItem
                    role="menuitemcheckbox"
                    aria-checked={includeTests}
                    onClick={() => {
                        onChange(!includeTests);
                        setAnchor(null);
                    }}
                >
                    <ListItemIcon>
                        {includeTests ? <CheckBox fontSize="small" /> : <CheckBoxOutlineBlank fontSize="small" />}
                    </ListItemIcon>
                    <ListItemText>{tasks ? 'Testaufgaben anzeigen' : 'Testvorgänge anzeigen'}</ListItemText>
                </MenuItem>
            </Menu>
        </>
    );
}
