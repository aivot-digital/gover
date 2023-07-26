import React, {useState} from 'react';
import {Box, FormControlLabel, IconButton, Menu, MenuItem, Switch, Tooltip, Typography} from '@mui/material';
import {selectUseIdsInComponentTree, selectUseTestMode, selectWarnDuplicateIds, setExpandElementTree, toggleIdsInComponentTree, toggleTestMode, toggleWarnDuplicateIds} from '../../../../slices/admin-settings-slice';
import {useTheme} from '@mui/material/styles';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {ElementEditor} from '../element-editor/element-editor';
import {type ElementTreeHeaderProps} from './element-tree-header-props';
import {type RootElement} from '../../../../models/elements/root-element';
import {type Application} from '../../../../models/entities/application';
import {type Preset} from '../../../../models/entities/preset';
import {type GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import ExpandOutlinedIcon from '@mui/icons-material/ExpandOutlined';
import CompressOutlinedIcon from '@mui/icons-material/CompressOutlined';
import IntegrationInstructionsOutlinedIcon from '@mui/icons-material/IntegrationInstructionsOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import GradingOutlinedIcon from '@mui/icons-material/GradingOutlined';

export function ElementTreeHeader<T extends RootElement | GroupLayout, E extends Application | Preset>(props: ElementTreeHeaderProps<T, E>): JSX.Element {
    const dispatch = useAppDispatch();

    const testMode = useAppSelector(selectUseTestMode);
    const useIdsInComponentTree = useAppSelector(selectUseIdsInComponentTree);
    const warnDuplicateIds = useAppSelector(selectWarnDuplicateIds);

    const [showEditor, setShowEditor] = useState(false);
    const [cTMenuAnchorEl, setCTMenuAnchorEl] = useState<null | HTMLElement>(null);

    const handleOpenCTMenu = (event: React.MouseEvent<HTMLButtonElement>): void => {
        setCTMenuAnchorEl(event.currentTarget);
    };

    const handleCloseCTMenu = (): void => {
        setCTMenuAnchorEl(null);
    };

    const theme = useTheme();

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mt: 1,
                    mb: 1,
                    padding: 1,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                    }}
                >
                    <AccountTreeOutlinedIcon
                        sx={{
                            color: theme.palette.primary.dark,
                        }}
                    />

                    <Typography
                        variant="h4"
                        color="primary"
                        sx={{ml: 2}}
                    >
                        Struktur
                    </Typography>

                    {
                        testMode &&
                        props.element.testProtocolSet == null &&
                        <Tooltip title="Fachliche Prüfung ausstehend">
                            <IconButton
                                sx={{
                                    ml: 1,
                                }}
                            >
                                <GradingOutlinedIcon
                                    fontSize="small"
                                    color="warning"
                                />
                            </IconButton>
                        </Tooltip>
                    }
                </Box>

                <Box>
                    <Tooltip
                        title="Suchen"
                        arrow
                    >
                        <IconButton
                            size="small"
                            onClick={props.onToggleSearch}
                        >
                            <SearchOutlinedIcon/>
                        </IconButton>
                    </Tooltip>

                    <Tooltip
                        title="Alles ausklappen"
                        arrow
                    >
                        <IconButton
                            size="small"
                            onClick={() => dispatch(setExpandElementTree('expanded'))}
                        >
                            <ExpandOutlinedIcon/>
                        </IconButton>
                    </Tooltip>

                    <Tooltip
                        title="Alles einklappen"
                        arrow
                    >
                        <IconButton
                            size="small"
                            onClick={() => dispatch(setExpandElementTree('collapsed'))}
                        >
                            <CompressOutlinedIcon/>
                        </IconButton>
                    </Tooltip>

                    <Tooltip
                        title="Einstellungen für Entwickler:innen"
                        arrow
                    >
                        <IconButton
                            size="small"
                            onClick={handleOpenCTMenu}
                        >
                            <IntegrationInstructionsOutlinedIcon/>
                        </IconButton>
                    </Tooltip>
                    <Tooltip
                        title="Formular konfigurieren"
                        arrow
                    >
                        <IconButton
                            size="small"
                            onClick={() => {
                                setShowEditor(true);
                            }}
                            sx={{
                                marginRight: '7px',
                            }}
                        >
                            <SettingsOutlinedIcon/>
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
                        label="Warnungen für doppelte IDs anzeigen"
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
                        label="Prüfungsansicht aktivieren"
                    />
                </MenuItem>
            </Menu>

            {
                showEditor &&
                <ElementEditor
                    parents={[] /* Uppermost element so no parents here */}
                    entity={props.entity}
                    element={props.entity.root as any /* TODO: Fix this any type */}
                    onSave={(updatedElement: Partial<T>, updatedApplication: Partial<E>) => {
                        setShowEditor(false);
                        props.onPatch(updatedElement, updatedApplication);
                    }}
                    onCancel={() => {
                        setShowEditor(false);
                    }}
                    editable={props.editable}
                />
            }
        </>
    );
}
