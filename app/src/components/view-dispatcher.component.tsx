import React, {ComponentType, useCallback, useMemo, useState} from 'react';
import Grid from '@mui/material/Grid';
import {type AnyElement} from '../models/elements/any-element';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {views as Views} from '../views';
import {type BaseViewProps} from '../views/base-view';
import IconButton from '@mui/material/IconButton';
import {ElementErrorBoundary} from './element-error-boundary/element-error-boundary';
import {resolveErrors, resolveOverride, resolveValueForResolvedOverride, resolveVisibility} from '../utils/element-data-utils';
import {useElementEditorNavigationActions} from '../hooks/use-element-editor-navigation';
import {isAnyContentElement} from '../models/elements/form/content/any-content-element';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import {Box, Divider, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import {ContentPaste, Edit} from '@mui/icons-material';
import {showErrorSnackbar, showSuccessSnackbar} from '../slices/snackbar-slice';
import {useAppDispatch} from '../hooks/use-app-dispatch';
import {useAppSelector} from '../hooks/use-app-selector';
import {selectDisableElementContextMenu, setComponentTree} from '../slices/admin-settings-slice';
import {generateComponentTitle} from '../utils/generate-component-title';
import JumpToElement from '@aivot/mui-material-symbols-400-outlined/dist/jump-to-element/JumpToElement';
import {copyToClipboardText} from '../utils/copy-to-clipboard';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../models/element-data';

interface DispatcherComponentProps<T extends AnyElement> {
    rootElement: AnyElement;
    allElements: AnyElement[];
    element: T;

    scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
    isBusy: boolean;
    isDeriving: boolean;
    mode: 'editor' | 'viewer';

    authoredElementValues: AuthoredElementValues;
    derivedData: DerivedRuntimeElementData;
    rootAuthoredElementValues?: AuthoredElementValues;
    rootDerivedData?: DerivedRuntimeElementData;
    onAuthoredElementValuesChange: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;
    onElementBlur?: (data: AuthoredElementValues, triggeringElementIds: string[]) => void;
    onDerivedDataChange?: (data: DerivedRuntimeElementData) => void;

    disableVisibility?: boolean;
    derivationTriggerIdQueue: string[];
}

export function ViewDispatcherComponent<T extends AnyElement>(props: DispatcherComponentProps<T>) {
    const disableElementContextMenu = useAppSelector(selectDisableElementContextMenu);

    const {
        rootElement,
        element: initialElement,
        allElements,
        scrollContainerRef,
        isBusy: baseIsBusy,
        isDeriving: baseIsDeriving,
        mode,
        authoredElementValues,
        derivedData,
        rootAuthoredElementValues,
        rootDerivedData,
        onAuthoredElementValuesChange,
        onElementBlur,
        onDerivedDataChange,
        disableVisibility,
        derivationTriggerIdQueue,
    } = props;

    const {
        id: elementId,
    } = initialElement;

    const element: AnyElement = useMemo(() => {
        return resolveOverride(initialElement, derivedData) as AnyElement;
    }, [initialElement, derivedData]);

    const value = useMemo(() => {
        return resolveValueForResolvedOverride(element, authoredElementValues, derivedData);
    }, [element, authoredElementValues, derivedData]);

    const error: string[] | undefined | null = useMemo(() => {
        return resolveErrors(element, derivedData);
    }, [element, derivedData]);

    const effectiveRootAuthoredElementValues = useMemo(() => {
        return rootAuthoredElementValues ?? authoredElementValues;
    }, [rootAuthoredElementValues, authoredElementValues]);

    const effectiveRootDerivedData = useMemo(() => {
        return rootDerivedData ?? derivedData;
    }, [rootDerivedData, derivedData]);

    const handleSetValue = useCallback((updatedValue: any | null | undefined, triggeringElementIds?: string[]) => {
        if (updatedValue == value) {
            return;
        }

        const newAuthoredElementValues = {
            ...authoredElementValues,
            [elementId]: updatedValue ?? null,
        };

        onAuthoredElementValuesChange(newAuthoredElementValues, [elementId, ...(triggeringElementIds ?? [])]);
    }, [value, authoredElementValues, onAuthoredElementValuesChange, elementId]);

    const handleOnBlur = useCallback((updatedValue: any | null | undefined, triggeringElementIds?: string[]) => {
        if (updatedValue == value || onElementBlur == null) {
            return;
        }

        const newAuthoredElementValues = {
            ...authoredElementValues,
            [elementId]: updatedValue ?? null,
        };

        onElementBlur(newAuthoredElementValues, [elementId, ...(triggeringElementIds ?? [])]);
    }, [value, authoredElementValues, onElementBlur, elementId]);

    const Component: ComponentType<BaseViewProps<typeof element, any>> | null = useMemo(() => Views[element.type], [element.type]);

    const isVisible = useMemo(() => {
        if (isAnyInputElement(element) && element.technical && (mode !== 'editor' || !disableVisibility)) {
            return false;
        }

        return resolveVisibility(element, derivedData);
    }, [derivedData, element, mode, disableVisibility]);

    const isBusy: boolean = useMemo(() => {
        return baseIsBusy || baseIsDeriving && (
            (element.visibility?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false) ||
            (element.override?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false) ||
            (isAnyInputElement(element) && (element.value?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false))
        );
    }, [baseIsBusy, baseIsDeriving, element]);

    const viewProps: BaseViewProps<typeof element, any> = useMemo(() => ({
        rootElement: rootElement,
        allElements: allElements,
        element: element,
        setValue: handleSetValue,
        onBlur: handleOnBlur,
        errors: error,
        value: value,
        scrollContainerRef: scrollContainerRef,
        isBusy: isBusy,
        isDeriving: baseIsDeriving,
        mode: mode,
        authoredElementValues: authoredElementValues,
        derivedData: derivedData,
        rootAuthoredElementValues: effectiveRootAuthoredElementValues,
        rootDerivedData: effectiveRootDerivedData,
        onAuthoredElementValuesChange: onAuthoredElementValuesChange,
        onElementBlur: onElementBlur,
        onDerivedDataChange: onDerivedDataChange,
        disableVisibility: disableVisibility,
        derivationTriggerIdQueue: derivationTriggerIdQueue,
    }), [
        allElements,
        element,
        handleSetValue,
        handleOnBlur,
        error,
        value,
        scrollContainerRef,
        isBusy,
        baseIsDeriving,
        mode,
        authoredElementValues,
        derivedData,
        effectiveRootAuthoredElementValues,
        effectiveRootDerivedData,
        onAuthoredElementValuesChange,
        onElementBlur,
        onDerivedDataChange,
        disableVisibility,
        derivationTriggerIdQueue,
    ]);



    if (Component == null || !isVisible) {
        return null;
    }

    return (
        <Grid
            id={elementId}
            data-initial-id={elementId /* TODO: Remove here and where referenced */}
            data-resolved-id={elementId /* TODO: Remove here and where referenced */}
            sx={{
                position: 'relative',
                '&:hover > .editor-element-context-menu, &:hover > .editor-element-context-menu-cutout': {
                    display: mode === 'editor' && !disableElementContextMenu ? 'block' : 'none',
                },
            }}
            size={{
                xs: 12,
                md: ('weight' in element && element.weight != null) ? element.weight : 12,
            }}
        >
            {
                mode === 'editor' &&
                (
                    isAnyInputElement(element) ||
                    isAnyContentElement(element)
                ) &&
                <>
                    <ContextMenuButton
                        element={element}
                    />
                </>
            }

            <ElementErrorBoundary viewProps={viewProps}>
                <Component {...viewProps} />
            </ElementErrorBoundary>
        </Grid>
    );
}

interface ContextMenuButtonProps {
    element: AnyElement;
}

function ContextMenuButton(props: ContextMenuButtonProps) {
    const {
        element,
    } = props;

    const dispatch = useAppDispatch();
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const open = Boolean(anchorEl);

    const {
        navigateToElementEditor,
    } = useElementEditorNavigationActions();

    const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        event.stopPropagation();
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleEdit = () => {
        dispatch(setComponentTree(true));
        navigateToElementEditor(element.id);
        handleMenuClose();
    };

    const handleJumpTo = () => {
        dispatch(setComponentTree(true));
        navigateToElementEditor(element.id, null);
        handleMenuClose()
    };

    const handleCopyId = async () => {
        const success = await copyToClipboardText(element.id ?? '');
        if (success) {
            dispatch(showSuccessSnackbar('Element-ID in Zwischenablage kopiert'));
        } else {
            dispatch(showErrorSnackbar('Element-ID konnte nicht in Zwischenablage kopiert werden'));
        }

        handleMenuClose();
    };

    const elementTitle = generateComponentTitle(element);

    return (
        <>
            <Box
                className="editor-element-context-menu-cutout"
                sx={{
                    position: 'absolute',
                    top: 6,
                    right: -13,
                    zIndex: 9,
                    display: 'none',
                    height: 24,
                    width: 24,
                    backgroundColor: 'rgb(255,255,255)',
                    borderRadius: '50%',
                    transform: 'scale(1.25)',
                }}
            />
            <IconButton
                className="editor-element-context-menu"
                onClick={handleMenuOpen}
                size="small"
                color="primary"
                sx={{
                    position: 'absolute',
                    top: 6,
                    right: -13,
                    zIndex: 10,
                    display: 'none',
                    p: 0.25,
                    height: 24,
                    width: 24,
                    lineHeight: '24px',
                    backgroundColor: 'rgba(0,0,0,.05)',
                }}
            >
                <MoreVert sx={{fontSize: '1.25rem'}} />
            </IconButton>

            <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleMenuClose}
                anchorOrigin={{vertical: 'bottom', horizontal: 'right'}}
                transformOrigin={{vertical: 'top', horizontal: 'right'}}
            >
                <Box
                    sx={{
                        px: 2,
                        pt: .25,
                        pb: 0.75,
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 0.25,
                    }}
                >
                    <Typography
                        variant="body1"
                        sx={{
                            fontWeight: 600,
                            color: '#111',
                            lineHeight: 1.2,
                            maxWidth: 200,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            display: 'block',
                        }}
                        title={elementTitle}
                    >
                        {elementTitle}
                    </Typography>
                    <Typography
                        variant="caption"
                        sx={{
                            color: 'rgba(0,0,0,0.5)',
                            fontWeight: 500,
                            mb: '-2px',
                            maxWidth: 200,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            display: 'block',
                        }}
                        title={element.id}
                    >
                        ID: {element.id}
                    </Typography>
                </Box>

                <Divider sx={{my: 1}} />

                <MenuItem
                    onClick={handleEdit}
                >
                    <ListItemIcon>
                        <Edit fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary="Bearbeiten" />
                </MenuItem>

                <MenuItem onClick={handleJumpTo}>
                    <ListItemIcon>
                        <JumpToElement fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary="Zum Element springen" />
                </MenuItem>

                <MenuItem onClick={handleCopyId}>
                    <ListItemIcon>
                        <ContentPaste fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary="Element-ID kopieren" />
                </MenuItem>
            </Menu>
        </>
    );
}
