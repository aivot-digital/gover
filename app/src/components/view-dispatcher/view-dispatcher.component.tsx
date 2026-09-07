import React, {ComponentType, useCallback, useMemo, useRef, useState} from 'react';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import {Box, Divider, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import ContentPaste from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import JumpToElement from '@aivot/mui-material-symbols-400-n25-outlined/JumpToElement';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import {BaseViewProps} from '../../views/base-view';
import {AnyElement} from '../../models/elements/any-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectDisableElementContextMenu, setComponentTree} from '../../slices/admin-settings-slice';
import {ElementErrorBoundary} from '../element-error-boundary/element-error-boundary';
import {
    resolveDisabled,
    resolveErrorDetails,
    resolveErrors,
    resolveOverride,
    resolveValueForResolvedOverride,
    resolveVisibility,
} from '../../utils/element-data-utils';
import {useViewDispatcherContext, ViewDispatcherMode} from './view-dispatcher.context';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {views as Views} from '../../views';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useElementTreeInlineEditorContext} from '../element-tree-2/components/element-tree-inline-editor-context';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {getPreviewHighlightStyles} from './preview-highlight-styles';
import {isSectionElementType} from '../../models/elements/steps/step-element';
import {ElementType} from '../../data/element-type/element-type';
import {
    getInputModeVariableCategoryLabel,
    getInputModeVariableReference,
    InputModeField,
    type InputModeVariable,
} from '../input-mode-field/input-mode-field';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {type BaseInputElement} from '../../models/elements/form/base-input-element';
import {type AuthoredInputValue, type InputModePolicy} from '../../models/input-mode';
import {literalAuthoredValue} from '../../models/element-data';
import type {DynamicTextInputMethods} from '../dynamic-text/dynamic-text-metadata';

type Props<T extends AnyElement> = Omit<BaseViewProps<T, any>, 'value' | 'setValue' | 'onBlur' | 'errors' | 'errorDetails'>

const ElementWrapperClassName = 'editor-element-wrapper';
// Parent wrappers are still hovered when the pointer is over a child. Suppress the
// parent's button in that case so only the innermost allowed element shows a menu.
const ElementContextMenuHoverSelector = [
    `&:hover:not(:has(.${ElementWrapperClassName}:hover)) > .editor-element-context-menu`,
    `&:hover:not(:has(.${ElementWrapperClassName}:hover)) > .editor-element-context-menu-cutout`,
].join(', ');
// These larger structural elements either have awkward hit areas or can make child menus unreachable.
const DeniedContextMenuElementTypes = new Set<ElementType>([
    ElementType.Step,
    ElementType.IntroductionStep,
    ElementType.SubmitStep,
    ElementType.SummaryStep,
    ElementType.GroupLayout,
]);
const InputModeCompatibleElementTypes = new Set<ElementType>([
    ElementType.Text,
    ElementType.Number,
    ElementType.Select,
    ElementType.Radio,
    ElementType.RichTextInput,
    ElementType.ReplicatingContainer,
]);
const DynamicTextCompatibleElementTypes = new Set<ElementType>([
    ElementType.Text,
    ElementType.RichTextInput,
]);

export function ViewDispatcherComponent<T extends AnyElement>(props: Props<T>) {
    const disableElementContextMenu = useAppSelector(selectDisableElementContextMenu);
    const dynamicTextInputRef = useRef<DynamicTextInputMethods | null>(null);
    const registerDynamicTextInput = useCallback((input: DynamicTextInputMethods | null) => {
        dynamicTextInputRef.current = input;
    }, []);
    const insertDynamicTextVariable = useCallback((variable: InputModeVariable) => {
        const insert = () => dynamicTextInputRef.current?.insertVariableReference(
            getInputModeVariableReference(variable),
        );
        if (typeof requestAnimationFrame === 'function') {
            requestAnimationFrame(insert);
        } else {
            insert();
        }
    }, []);

    const {
        element: initialElement,
        isBusy: baseIsBusy,
        isDeriving: baseIsDeriving,
        authoredElementValues,
        derivedData,
        onAuthoredElementValuesChange,
        onElementBlur,
        derivationTriggerIdQueue,
        suppressErrors,
    } = props;

    const {
        mode,
        rootElement,
        rootAuthoredElementValues,
        rootDerivedData,
        showInvisibleElements,
        highlightedElementId,
        inputModesEnabled,
        inputModeVariables,
    } = useViewDispatcherContext();

    const {
        id: elementId,
    } = initialElement;

    const element: AnyElement = useMemo(() => {
        return resolveOverride(initialElement, derivedData) as AnyElement;
    }, [initialElement, derivedData]);

    const value = useMemo(() => {
        return resolveValueForResolvedOverride(element, authoredElementValues, derivedData);
    }, [element, authoredElementValues, derivedData]);
    const authoredValue = authoredElementValues[elementId];

    const disabled: boolean = useMemo(() => {
        return resolveDisabled(element, derivedData);
    }, [element, derivedData]);

    const resolvedErrors: string[] | undefined | null = useMemo(() => {
        return resolveErrors(element, derivedData);
    }, [element, derivedData]);

    const resolvedErrorDetails: string[] | undefined | null = useMemo(() => {
        return resolveErrorDetails(element, derivedData);
    }, [element, derivedData]);

    const effectiveRootAuthoredElementValues = useMemo(() => {
        return rootAuthoredElementValues ?? authoredElementValues;
    }, [rootAuthoredElementValues, authoredElementValues]);

    const effectiveRootDerivedData = useMemo(() => {
        return rootDerivedData ?? derivedData;
    }, [rootDerivedData, derivedData]);

    const handleSetAuthoredValue = useCallback((updatedValue: AuthoredInputValue<unknown>, triggeringElementIds?: string[]) => {
        if (isUnchangedAuthoredValue(authoredValue, updatedValue)) {
            return;
        }

        const newAuthoredElementValues = {
            ...authoredElementValues,
            [elementId]: updatedValue,
        };

        onAuthoredElementValuesChange(newAuthoredElementValues, [elementId, ...(triggeringElementIds ?? [])]);
    }, [authoredValue, authoredElementValues, onAuthoredElementValuesChange, elementId]);

    const handleAuthoredValueBlur = useCallback((updatedValue: AuthoredInputValue<unknown>, triggeringElementIds?: string[]) => {
        if (isUnchangedAuthoredValue(authoredValue, updatedValue) || onElementBlur == null) {
            return;
        }

        const newAuthoredElementValues = {
            ...authoredElementValues,
            [elementId]: updatedValue,
        };

        onElementBlur(newAuthoredElementValues, [elementId, ...(triggeringElementIds ?? [])]);
    }, [authoredValue, authoredElementValues, onElementBlur, elementId]);

    const handleSetLiteralValue = useCallback((updatedValue: any | null, triggeringElementIds?: string[]) => {
        handleSetAuthoredValue(literalAuthoredValue(updatedValue), triggeringElementIds);
    }, [handleSetAuthoredValue]);

    const handleLiteralValueBlur = useCallback((updatedValue: any | null, triggeringElementIds?: string[]) => {
        handleAuthoredValueBlur(literalAuthoredValue(updatedValue), triggeringElementIds);
    }, [handleAuthoredValueBlur]);

    const ViewComponent: ComponentType<BaseViewProps<typeof element, any>> | null = useMemo(() => Views[element.type], [element.type]);

    const isVisible = useMemo(() => {
        if (showInvisibleElements) {
            return true;
        }

        if (isAnyInputElement(element) && element.technical) {
            return false;
        }

        return resolveVisibility(element, derivedData);
    }, [derivedData, element, mode, showInvisibleElements]);

    const isBusy: boolean = useMemo(() => {
        return baseIsBusy || baseIsDeriving && (
            (element.visibility?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false) ||
            (element.override?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false) ||
            (isAnyInputElement(element) && (element.value?.referencedIds?.some(refId => derivationTriggerIdQueue.includes(refId)) ?? false))
        );
    }, [baseIsBusy, baseIsDeriving, derivationTriggerIdQueue, element]);

    const isHighlightedInPreview = highlightedElementId === elementId &&
        !isSectionElementType(element.type);

    if (!isVisible) {
        return null;
    }

    if (ViewComponent == null) {
        return null;
    }

    const inputModePolicy = inputModesEnabled && isAnyInputElement(initialElement) &&
        InputModeCompatibleElementTypes.has(element.type)
        ? initialElement.inputModePolicy
        : null;
    const dynamicTextPolicy = inputModesEnabled && isAnyInputElement(initialElement) &&
        DynamicTextCompatibleElementTypes.has(element.type)
        ? initialElement.dynamicTextPolicy
        : null;
    const rendersInputMode = inputModePolicy != null || dynamicTextPolicy != null;
    const resolvedInputElement = element as BaseInputElement<ElementType>;
    const hasAuthoredValue = Object.prototype.hasOwnProperty.call(authoredElementValues, elementId);
    const inputModeValue = inputModePolicy == null
        ? literalAuthoredValue(value)
        : resolveInitialInputModeValue(hasAuthoredValue, authoredValue, value, inputModePolicy);
    const dynamicTextVariableMetadata = (inputModeVariables ?? [])
        .filter((variable) => dynamicTextPolicy?.variableSuggestionSources.includes(variable.source))
        .map((variable) => ({
            reference: getInputModeVariableReference(variable),
            label: variable.label,
            category: getInputModeVariableCategoryLabel(variable.source),
            origin: variable.origin?.name ?? undefined,
            description: variable.description ?? undefined,
        }));

    const view = (viewValue: any, setViewValue: typeof handleSetLiteralValue, blurViewValue: typeof handleLiteralValueBlur, inputModeLiteralContext?: BaseViewProps<typeof element, any>['inputModeLiteralContext']) => (
        <ViewComponent
            {...props}
            element={element}
            value={viewValue}
            setValue={setViewValue}
            onBlur={blurViewValue}
            errors={suppressErrors ? undefined : resolvedErrors}
            errorDetails={suppressErrors ? undefined : resolvedErrorDetails}
            isBusy={isBusy || disabled}
            isDeriving={baseIsDeriving}
            inputModeLiteralContext={inputModeLiteralContext}
        />
    );

    return (
        <Grid
            className={ElementWrapperClassName}
            id={elementId}
            data-initial-id={elementId /* TODO: Remove here and where referenced */}
            data-resolved-id={elementId /* TODO: Remove here and where referenced */}
            sx={(theme) => ({
                position: 'relative',
                [ElementContextMenuHoverSelector]: {
                    display: mode === ViewDispatcherMode.Editor && !disableElementContextMenu ? 'block' : 'none',
                },
                ...getPreviewHighlightStyles(theme, isHighlightedInPreview),
            })}
            size={{
                xs: 12,
                md: ('weight' in element && element.weight != null) ? element.weight : 12,
            }}
        >
            <ContextMenuButton
                element={element}
            />

            <ElementErrorBoundary element={element} >
                {rendersInputMode ? <InputModeField
                    label={resolvedInputElement.label ?? ''}
                    hint={resolvedInputElement.hint ?? undefined}
                    error={suppressErrors || resolvedErrors == null ? undefined : resolvedErrors.join(' ')}
                    required={resolvedInputElement.required ?? undefined}
                    readOnly={baseIsBusy || disabled}
                    busy={baseIsDeriving && isBusy}
                    allowedModes={inputModePolicy?.allowedModes ?? ['Literal']}
                    allowedVariableSources={inputModePolicy?.allowedVariableSources}
                    dynamicTextVariableSources={dynamicTextPolicy?.variableSuggestionSources}
                    variables={inputModeVariables ?? []}
                    value={inputModeValue}
                    onChange={(nextValue, triggeringElementIds) => handleSetAuthoredValue(nextValue, triggeringElementIds)}
                    onInsertVariable={dynamicTextPolicy == null ? undefined : insertDynamicTextVariable}
                    rootElement={rootElement}
                    noCodeReturnType={resolveNoCodeReturnType(element.type)}
                    renderLiteral={({value: literalValue, onChange, variableInsertAction, fieldProps}) => view(
                        literalValue,
                        (nextValue, triggeringElementIds) => onChange(nextValue, triggeringElementIds),
                        (nextValue, triggeringElementIds) => handleAuthoredValueBlur(
                            literalAuthoredValue(nextValue),
                            triggeringElementIds,
                        ),
                        {
                            fieldProps,
                            variableInsertAction,
                            dynamicText: dynamicTextPolicy == null ? undefined : {
                                inputRef: registerDynamicTextInput,
                                variableMetadata: dynamicTextVariableMetadata,
                            },
                        },
                    )}
                /> : view(value, handleSetLiteralValue, handleLiteralValueBlur)}
            </ElementErrorBoundary>
        </Grid>
    );
}

function isUnchangedAuthoredValue(previous: AuthoredInputValue<unknown> | undefined, next: AuthoredInputValue<unknown>): boolean {
    // Controls create a new Literal wrapper even on an unchanged blur. Compare payload identity as before the
    // wrapper conversion; deep comparison would incorrectly consider different File instances equal.
    return previous === next || previous?.type === 'Literal' && next.type === 'Literal' && previous.value === next.value;
}

function resolveNoCodeReturnType(elementType: ElementType): NoCodeDataType {
    if (elementType === ElementType.Number) return NoCodeDataType.Number;
    if (elementType === ElementType.ReplicatingContainer) return NoCodeDataType.List;
    return NoCodeDataType.String;
}

function resolveInitialInputModeValue(
    hasAuthoredValue: boolean,
    authoredValue: unknown,
    effectiveValue: unknown,
    policy: InputModePolicy,
): unknown {
    if (hasAuthoredValue) {
        return authoredValue;
    }

    switch (policy.defaultMode ?? 'Literal') {
        case 'Variable':
            return {
                type: 'Variable',
                reference: {source: policy.allowedVariableSources?.[0] ?? 'ProcessData', path: ''},
            } satisfies AuthoredInputValue<unknown>;
        case 'NoCode':
            return {
                type: 'NoCode',
                operand: {type: 'NoCodeStaticValue', value: null},
            } satisfies AuthoredInputValue<unknown>;
        case 'LowCode':
            return {type: 'LowCode', code: ''} satisfies AuthoredInputValue<unknown>;
        case 'Literal':
            return literalAuthoredValue(effectiveValue);
    }
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

    const inlineEditorContext = useElementTreeInlineEditorContext();

    const {
        mode,
        rootElement,
    } = useViewDispatcherContext();

    const isRootDispatcherElement = element.id === rootElement.id;
    const isContextMenuDeniedElement = DeniedContextMenuElementTypes.has(element.type);
    const canShowContextMenu = inlineEditorContext != null &&
        mode === ViewDispatcherMode.Editor &&
        !isRootDispatcherElement &&
        !isContextMenuDeniedElement;

    if (!canShowContextMenu) {
        return null;
    }

    const {
        navigateToElementEditor,
        highlightElementInTree,
        cloneElement,
        deleteElement,
        editable,
    } = inlineEditorContext;

    const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        event.stopPropagation();
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleEdit = () => {
        dispatch(setComponentTree(true));
        navigateToElementEditor(element);
        handleMenuClose();
    };

    const handleHighlightInTree = () => {
        dispatch(setComponentTree(true));
        highlightElementInTree(element);
        handleMenuClose();
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

    const handleCloneElement = () => {
        cloneElement(element);
        handleMenuClose();
    };

    const handleDeleteElement = () => {
        deleteElement(element);
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
                    backgroundColor: 'background.paper',
                    // Match the effective Paper surface in dark mode, including its elevation overlay.
                    backgroundImage: 'var(--Paper-overlay)',
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
                    backgroundColor: 'action.hover',
                }}
            >
                <MoreVert sx={{fontSize: '1.25rem'}}/>
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
                            color: 'text.primary',
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
                            color: 'text.secondary',
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

                <Divider sx={{my: 1}}/>

                <MenuItem
                    onClick={handleEdit}
                >
                    <ListItemIcon>
                        <Edit fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary={editable ? 'Element bearbeiten' : 'Element anzeigen'}/>
                </MenuItem>

                <MenuItem onClick={handleHighlightInTree}>
                    <ListItemIcon>
                        <JumpToElement fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary="Element in Struktur hervorheben"/>
                </MenuItem>

                <MenuItem onClick={handleCopyId}>
                    <ListItemIcon>
                        <ContentPaste fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary="Element-ID kopieren"/>
                </MenuItem>

                <MenuItem
                    onClick={handleCloneElement}
                    disabled={!editable}
                >
                    <ListItemIcon>
                        <ContentCopy fontSize="small"/>
                    </ListItemIcon>
                    <ListItemText primary="Element duplizieren"/>
                </MenuItem>

                <Divider/>

                <MenuItem
                    onClick={handleDeleteElement}
                    disabled={!editable}
                >
                    <ListItemIcon>
                        <Delete fontSize="small"
                                color="error"/>
                    </ListItemIcon>
                    <ListItemText
                        primary="Element löschen"
                        sx={{
                            color: 'error.main',
                        }}
                    />
                </MenuItem>
            </Menu>
        </>
    );
}
