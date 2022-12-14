import {Box, FormControlLabel, IconButton, Menu, MenuItem, Switch, Tooltip, Typography} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faGear, faListTree, faMessageCode} from '@fortawesome/pro-light-svg-icons';
import {
    selectUseIdsInComponentTree,
    selectUseTestMode,
    selectWarnDuplicateIds,
    toggleIdsInComponentTree,
    toggleTestMode,
    toggleWarnDuplicateIds
} from '../../../../slices/admin-settings-slice';
import React, {useState} from 'react';
import {useTheme} from '@mui/material/styles';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {ElementEditor} from '../element-editor/element-editor';
import {ElementTreeHeaderProps} from './element-tree-header-props';
import {AnyElement} from '../../../../models/elements/any-element';

export function ElementTreeHeader<T extends AnyElement>(props: ElementTreeHeaderProps<T>) {
    const dispatch = useAppDispatch();

    const testMode = useAppSelector(selectUseTestMode);
    const useIdsInComponentTree = useAppSelector(selectUseIdsInComponentTree);
    const warnDuplicateIds = useAppSelector(selectWarnDuplicateIds);

    const [showEditor, setShowEditor] = useState(false);
    const [cTMenuAnchorEl, setCTMenuAnchorEl] = useState<null | HTMLElement>(null);

    const handleOpenCTMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setCTMenuAnchorEl(event.currentTarget);
    };

    const handleCloseCTMenu = () => {
        setCTMenuAnchorEl(null);
    };

    const theme = useTheme();

    return (
        <>
            <Box sx={{display: 'flex', justifyContent: 'space-between', mt: 1, mb: 1}}>
                <Box sx={{display: 'flex', alignItems: 'center'}}>

                    <span
                        style={{
                            color: theme.palette.primary.dark,
                            fontSize: '1.625rem',
                        }}
                    >
                        <FontAwesomeIcon icon={faListTree}/>
                    </span>

                    <Typography
                        variant="h6"
                        color="primary"
                        sx={{ml: 1, mt: '2px'}}
                    >
                        Struktur
                    </Typography>
                </Box>

                <Box>
                    <Tooltip
                        title="Einstellungen f??r Entwickler:innen"
                        arrow
                    >
                        <IconButton onClick={handleOpenCTMenu}>
                            <FontAwesomeIcon icon={faMessageCode}/>
                        </IconButton>
                    </Tooltip>
                    <Tooltip
                        title="Antrag konfigurieren"
                        arrow
                    >
                        <IconButton
                            onClick={() => {
                                setShowEditor(true);
                            }}
                            sx={{marginRight: '7px'}}
                        >
                            <FontAwesomeIcon icon={faGear}/>
                        </IconButton>
                    </Tooltip>
                </Box>
            </Box>

            <Menu
                id="basic-menu"
                anchorEl={cTMenuAnchorEl}
                open={cTMenuAnchorEl != null}
                onClose={handleCloseCTMenu}
            >
                <MenuItem>
                    <FormControlLabel
                        control={
                            <Switch
                                checked={useIdsInComponentTree}
                                onChange={() => {
                                    dispatch(toggleIdsInComponentTree());
                                }}
                            />
                        }
                        label="Element-IDs anstatt Titel anzeigen"
                    />
                </MenuItem>
                <MenuItem>
                    <FormControlLabel
                        control={
                            <Switch
                                checked={warnDuplicateIds}
                                onChange={() => {
                                    dispatch(toggleWarnDuplicateIds());
                                }}
                            />
                        }
                        label="Warnungen f??r doppelte IDs anzeigen"
                    />
                </MenuItem>
                <MenuItem>
                    <FormControlLabel
                        control={
                            <Switch
                                checked={testMode}
                                onChange={() => {
                                    dispatch(toggleTestMode());
                                }}
                            />
                        }
                        label="Pr??fungsansicht aktivieren"
                    />
                </MenuItem>
            </Menu>

            {
                showEditor &&
                <ElementEditor
                    element={props.element}
                    onSave={update => {
                        setShowEditor(false);
                        props.onPatch(update);
                    }}
                    onCancel={() => {
                        setShowEditor(false);
                    }}
                />
            }
        </>
    );
}
