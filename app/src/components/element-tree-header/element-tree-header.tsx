import React, {useEffect, useRef, useState} from 'react';
import {Box, Divider, FormControlLabel, IconButton, Menu, MenuItem, Switch, Tooltip, Typography} from '@mui/material';
import {
    selectTreeElementSearch,
    selectUseIdsInComponentTree,
    selectUseTestMode,
    selectWarnDuplicateIds,
    setElementTreeSearchLookupIndex,
    setExpandElementTree,
    setTreeElementSearch,
    toggleIdsInComponentTree,
    toggleTestMode,
    toggleWarnDuplicateIds,
} from '../../slices/admin-settings-slice';
import {styled, useTheme} from '@mui/material/styles';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {ElementEditor} from '../element-editor/element-editor';
import {type ElementTreeHeaderProps} from './element-tree-header-props';
import {type RootElement} from '../../models/elements/root-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import ExpandOutlinedIcon from '@mui/icons-material/ExpandOutlined';
import CompressOutlinedIcon from '@mui/icons-material/CompressOutlined';
import IntegrationInstructionsOutlinedIcon from '@mui/icons-material/IntegrationInstructionsOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import GradingOutlinedIcon from '@mui/icons-material/GradingOutlined';
import {type ElementTreeEntity} from '../element-tree/element-tree-entity';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import {SearchInput} from '../search-input-2/search-input';
import {isLoadedForm, selectAllElements} from '../../slices/app-slice';
import Fuse from 'fuse.js';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import {Actions} from '../actions/actions';
import {useElementEditorNavigation} from '../../hooks/use-element-editor-navigation';

const StyledBox = styled(Box)({
    position: 'relative',
    '&::after': {
        content: '""',
        position: 'absolute',
        bottom: '-20px',
        width: '100%',
        height: '20px',
        background: 'linear-gradient(180deg, rgba(255,255,255,1) 0%, rgba(255,255,255,0) 100%)',
    },
    '&::before': {
        content: '""',
        position: 'absolute',
        top: '-8px',
        width: '100%',
        height: '8px',
        backgroundColor: 'white',
    },
});

const SEARCH_DEBOUNCE_TIMEOUT = 600; // 2 seconds

export function ElementTreeHeader<T extends RootElement | GroupLayout, E extends ElementTreeEntity>(props: ElementTreeHeaderProps<T, E>) {
    const dispatch = useAppDispatch();

    const searchDebounceTimeout = useRef<NodeJS.Timeout>(undefined);

    const theme = useTheme();

    const {
        currentEditedElementId,
        navigateToElementEditor,
        closeElementEditor,
    } = useElementEditorNavigation();

    const testMode = useAppSelector(selectUseTestMode);
    const useIdsInComponentTree = useAppSelector(selectUseIdsInComponentTree);
    const warnDuplicateIds = useAppSelector(selectWarnDuplicateIds);
    const allElements = useAppSelector(selectAllElements);
    const searchResult = useAppSelector(selectTreeElementSearch);

    const [cTMenuAnchorEl, setCTMenuAnchorEl] = useState<null | HTMLElement>(null);

    const [search, setSearch] = useState<string>();

    const jumpTo = (elementId: string): void => {
        if (props.scrollContainerRef?.current) {
            const element = props
                .scrollContainerRef
                .current
                .querySelector(`[data-element-id="${elementId}"]`);

            if (element instanceof HTMLElement) {
                element.scrollIntoView({
                    behavior: 'smooth',
                    block: 'center',
                });
            }
        }
    };

    useEffect(() => {
        if (searchDebounceTimeout.current != null) {
            clearTimeout(searchDebounceTimeout.current);
            searchDebounceTimeout.current = undefined;
        }

        if (search == null || isStringNullOrEmpty(search)) {
            dispatch(setTreeElementSearch(undefined));
            return;
        }

        searchDebounceTimeout.current = setTimeout(() => {
            const resolvedElements = (allElements ?? [])
                .map((e) => ({
                    id: e.id,
                    title: generateComponentTitle(e),
                }));

            const fuse = new Fuse(resolvedElements, {
                keys: ['title', 'id'],
                threshold: 0.3,
                shouldSort: false,
            });

            const fundElementIds = fuse
                .search(search)
                .map(result => result.item.id);

            if (fundElementIds.length > 0) {
                jumpTo(fundElementIds[0]);
            }

            dispatch(setTreeElementSearch(fundElementIds));
        }, SEARCH_DEBOUNCE_TIMEOUT);
    }, [search]);

    const handleOpenCTMenu = (event: React.MouseEvent<HTMLButtonElement>): void => {
        setCTMenuAnchorEl(event.currentTarget);
    };

    const handleCloseCTMenu = (): void => {
        setCTMenuAnchorEl(null);
    };

    const handleToggleSearch = (): void => {
        if (search == null) {
            dispatch(setExpandElementTree('expanded'));
            dispatch(setTreeElementSearch([]));
            setSearch('');
        } else {
            dispatch(setTreeElementSearch(undefined));
            setSearch(undefined);
        }
    };

    const handlePreviousSearchResult = (): void => {
        if (search == null || searchResult == null || searchResult.foundIds == null || searchResult.foundIds.length === 0) {
            return;
        }

        const previousIndex = (searchResult.currentLookupIndex - 1 + (searchResult.foundIds.length ?? 0)) % (searchResult.foundIds.length ?? 0);
        const previousElementId = searchResult.foundIds[previousIndex];

        dispatch(setElementTreeSearchLookupIndex(previousIndex));

        jumpTo(previousElementId);
    };

    const handleNextSearchResult = (): void => {
        if (search == null || searchResult == null || searchResult.foundIds == null || searchResult.foundIds.length === 0) {
            return;
        }

        const nextIndex = (searchResult.currentLookupIndex + 1) % (searchResult.foundIds.length ?? 0);
        const nextElementId = searchResult.foundIds[nextIndex];

        dispatch(setElementTreeSearchLookupIndex(nextIndex));

        jumpTo(nextElementId);
    };

    return (
        <>
            <StyledBox
                sx={{
                    position: 'sticky',
                    top: '8px',
                    backgroundColor: 'white',
                    zIndex: 100,
                }}
            >
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
                                onClick={handleToggleSearch}
                            >
                                <SearchOutlinedIcon />
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
                                <ExpandOutlinedIcon />
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
                                <CompressOutlinedIcon />
                            </IconButton>
                        </Tooltip>

                        <Tooltip
                            title="Einstellungen für Entwickler:innen"
                            arrow
                        >
                            <IconButton
                                size="small"
                                onClick={handleOpenCTMenu}
                                sx={{position: 'relative'}}
                            >
                                <IntegrationInstructionsOutlinedIcon />
                            </IconButton>
                        </Tooltip>
                        <Tooltip
                            title={props.scope === 'application' ? 'Formular konfigurieren' : 'Vorlage konfigurieren'}
                            arrow
                        >
                            <IconButton
                                size="small"
                                onClick={() => {
                                    navigateToElementEditor(props.element.id);
                                }}
                                sx={{
                                    marginRight: '7px',
                                }}
                            >
                                <SettingsOutlinedIcon />
                            </IconButton>
                        </Tooltip>
                    </Box>
                </Box>
                <Divider
                    sx={{
                        mb: 2,
                        borderColor: '#16191F',
                    }}
                />
                {
                    search != null &&
                    <>
                        <Box
                            sx={{
                                display: 'flex',
                            }}
                        >
                            <SearchInput
                                value={search}
                                onChange={setSearch}
                                placeholder="Element Suchen"
                                sx={{
                                    flex: 1,
                                }}
                            />

                            <Actions
                                dense={true}
                                actions={[
                                    {
                                        tooltip: 'Nächstes Element',
                                        icon: <ArrowDownwardIcon />,
                                        onClick: handleNextSearchResult,
                                        disabled: searchResult?.foundIds == null || searchResult?.foundIds.length === 0,
                                    },
                                    {
                                        tooltip: 'Vorheriges Element',
                                        icon: <ArrowUpwardIcon />,
                                        onClick: handlePreviousSearchResult,
                                        disabled: searchResult?.foundIds == null || searchResult?.foundIds.length === 0,
                                    },
                                    {
                                        tooltip: 'Suche Schließen',
                                        icon: <CloseOutlinedIcon />,
                                        onClick: handleToggleSearch,
                                    },
                                ]}
                            />
                        </Box>

                        <Divider
                            sx={{
                                my: 2,
                                borderColor: '#16191F',
                            }}
                        />
                    </>
                }
            </StyledBox>

            <Menu
                id="basic-menu"
                anchorEl={cTMenuAnchorEl}
                open={cTMenuAnchorEl != null}
                onClose={handleCloseCTMenu}
                MenuListProps={{
                    'dense': true,
                }}
                anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'left',
                }}
                transformOrigin={{
                    vertical: 'top',
                    horizontal: 'left',
                }}
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
                currentEditedElementId === props.element.id &&
                <ElementEditor
                    open={true}
                    parents={[] /* Uppermost element so no parents here */}
                    entity={props.entity}
                    element={isLoadedForm(props.entity) ? props.entity.version.rootElement : props.entity.rootElement as any /* TODO: Fix this any type */}
                    onSave={(updatedElement: Partial<T>, updatedApplication: Partial<E>) => {
                        closeElementEditor();
                        props.onPatch(updatedElement, updatedApplication);
                    }}
                    onCancel={() => {
                        closeElementEditor();
                    }}
                    editable={props.editable}
                    scope={props.scope}
                    rootEditor={true}
                />
            }
        </>
    );
}
