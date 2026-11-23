import React, {type ReactNode, type RefObject, useCallback, useContext, useId, useMemo, useRef, useState} from 'react';
import {useDrag, useDrop} from 'react-dnd';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import {type Theme} from '@mui/material/styles';
import DragIndicator from '@aivot/mui-material-symbols-400-n25-outlined/DragIndicator';
import {stringOrDefault} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import AddCircleOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AddCircle';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {flattenElements} from '../../utils/flatten-elements';
import {
    type ReplicatingContainerElementValue,
    type ReplicatingContainerElementValues,
    resolveAuthoredReplicatingContainerElementValues,
    updateReplicatingContainerElementValues,
} from '../../models/element-data';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {resolveReplicatingContainerItemDerivedData} from '../../utils/element-data-utils';
import {ViewDispatcherComponent} from '../view-dispatcher/view-dispatcher.component';
import {
    ViewDispatcherContext,
    ViewDispatcherContextProvider,
    type ViewDispatcherRowScope,
} from '../view-dispatcher/view-dispatcher.context';
import {generateUUIDv7} from '../../utils/id-utils';
import {FormFieldGroup, type FormFieldGroupContext} from '../form-field';

export function ReplicatingContainerView(props: BaseViewProps<ReplicatingContainerLayout, ReplicatingContainerElementValues>) {
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const {
        element,
        value,
        setValue,
        onBlur,
        isDeriving,
        isBusy,
        derivedData,
        errors,
        derivationTriggerIdQueue,
    } = props;

    const {
        children,
    } = element;

    const isDisabled = useMemo(() => {
        if (element.disabled || props.inputModeLiteralContext?.fieldProps.disabled || props.inputModeLiteralContext?.fieldProps.readOnly) {
            return true;
        }

        if (isBusy || props.inputModeLiteralContext?.fieldProps.busy) {
            return true;
        }

        if (isDeriving && hasDerivableAspects(element)) {
            return true;
        }

        return false;
    }, [element, isBusy, isDeriving, props.inputModeLiteralContext?.fieldProps]);

    const minimumSets = Math.max(0, element.minimumRequiredSets ?? 0);
    // Optional collections must remain clearable, even while their non-empty value is below the validation minimum.
    const minRequiredSets = element.required === true ? Math.max(1, minimumSets) : 0;
    const displayedMinimumSets = Math.max(minimumSets, minRequiredSets);
    const hasEntries = (value?.length ?? 0) > 0;
    // Any entry may be removed, including the first, as long as the required minimum remains.
    const canRemoveEntries = (value?.length ?? 0) > minRequiredSets;
    const isAddDisabled = isDisabled || (element.maximumSets != null && element.maximumSets > 0 && (value ?? []).length >= element.maximumSets);
    const shouldShowEmptyState = !hasEntries;
    const isContainerBusy = isDeriving && hasDerivableAspects(element);
    const fieldError = errors != null && errors.length > 0 ? errors.join(' ') : undefined;
    const inputModeFieldProps = props.inputModeLiteralContext?.fieldProps;
    // Only unset or false counts as unsortable, so containers saved before the option existed keep their order.
    const isSortable = element.sortable === true && !isDisabled && (value?.length ?? 0) > 1;
    // Scoping the drag type to this container instance keeps rows from moving between containers.
    const rowDragType = `replicating-container-row:${useId()}`;

    const handleAdd = useCallback(() => {
        const updatedValue: ReplicatingContainerElementValues = [
            ...(value ?? []),
            {
                id: generateUUIDv7(),
                values: {},
            },
        ];

        const allChildIds = flattenElements(element, false)
            .map(child => child.id);

        setValue(updatedValue, allChildIds);
    }, [element, setValue, value]);

    const handleDelete = useCallback((_: ReplicatingContainerElementValue, index: number) => {
        const newValue = (value ?? [])
            .filter((_: ReplicatingContainerElementValue, i: number) => i !== index);

        const allChildIds = flattenElements(element, false)
            .map(child => child.id);

        setValue(newValue.length === 0 ? null : newValue, allChildIds);
        setConfirmDelete(undefined);
    }, [element, setValue, value]);

    const handleMove = useCallback((fromIndex: number, toIndex: number) => {
        const rows = [...(value ?? [])];
        if (fromIndex === toIndex || fromIndex < 0 || fromIndex >= rows.length || toIndex < 0 || toIndex >= rows.length) {
            return;
        }

        const [movedRow] = rows.splice(fromIndex, 1);
        rows.splice(toIndex, 0, movedRow);

        // Derived row data is resolved by position, so all rows must be derived again after a move.
        const allChildIds = flattenElements(element, false)
            .map(child => child.id);

        setValue(rows, allChildIds);
    }, [element, setValue, value]);

    return (
        <>
            <FormFieldGroup
                id={inputModeFieldProps?.id}
                label={inputModeFieldProps?.label ?? element.label ?? ''}
                hint={inputModeFieldProps?.hint ?? element.hint ?? undefined}
                error={inputModeFieldProps?.error ?? fieldError}
                required={inputModeFieldProps?.required ?? element.required ?? undefined}
                disabled={inputModeFieldProps?.disabled ?? Boolean(element.disabled || isBusy)}
                readOnly={inputModeFieldProps?.readOnly}
                busy={inputModeFieldProps?.busy ?? isContainerBusy}
                externalAction={inputModeFieldProps?.externalAction}
                labelAction={inputModeFieldProps?.labelAction}
                margin={inputModeFieldProps?.margin}
                sx={inputModeFieldProps?.sx}
                showOptionalIndicator={element.label != null && element.label.trim().length > 0}
            >
                {(fieldContext: FormFieldGroupContext) => (
                    <Box
                        data-replicating-container-list
                        sx={{
                            overflow: 'hidden',
                            border: '1px solid',
                            borderColor: fieldContext.invalid ? 'error.main' : 'divider',
                            borderRadius: 1,
                            backgroundColor: 'background.paper',
                        }}
                    >
                        {(value ?? []).map((val: ReplicatingContainerElementValue, valueIndex: number) => {
                            const rowValues = resolveAuthoredReplicatingContainerElementValues(val) ?? {};
                            const rowDerivedData = resolveReplicatingContainerItemDerivedData(element, derivedData, valueIndex);
                            const rowKey = val.id ?? valueIndex;
                            const itemLabelId = `${fieldContext.groupId}-item-${valueIndex}-label`;
                            const itemLabel = stringOrDefault(element.headlineTemplate, 'Datensatz #')
                                .replace('#', (valueIndex + 1).toFixed());

                            if (derivationTriggerIdQueue.includes(`${element.id}.${valueIndex}`)) {
                                return (
                                    <Box
                                        key={rowKey}
                                        role="status"
                                        aria-label={`${itemLabel} wird geladen`}
                                        aria-busy="true"
                                        sx={{
                                            borderTop: valueIndex > 0 ? '1px solid' : undefined,
                                            borderColor: 'divider',
                                        }}
                                    >
                                        <DatasetSkeleton size={children?.length}/>
                                    </Box>
                                );
                            }

                            const renderRow = (sortable?: SortableRowSlots) => (
                                <Box
                                    key={rowKey}
                                    ref={sortable?.rowRef}
                                    data-replicating-container-row
                                    role="group"
                                    aria-labelledby={itemLabelId}
                                    sx={{
                                        px: {xs: 2, sm: 2.5},
                                        pt: 2,
                                        pb: 2.5,
                                        borderTop: valueIndex > 0 ? '1px solid' : undefined,
                                        borderColor: 'divider',
                                        opacity: sortable?.isDragging ? 0.5 : undefined,
                                        // Marks the edge at which the dragged row would be inserted.
                                        boxShadow: sortable?.dropEdge != null ?
                                            (theme: Theme) => `inset 0 ${sortable.dropEdge === 'top' ? 2 : -2}px 0 ${theme.palette.primary.main}` :
                                            undefined,
                                    }}
                                >
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            flexWrap: 'wrap',
                                            rowGap: 1.25,
                                        }}
                                    >
                                        <Chip
                                            id={itemLabelId}
                                            label={itemLabel}
                                            size="small"
                                            variant="outlined"
                                            sx={{
                                                height: 24,
                                                borderColor: 'divider',
                                                backgroundColor: 'action.hover',
                                                '& .MuiChip-label': {
                                                    px: 1,
                                                    fontSize: '0.75rem',
                                                    fontWeight: 500,
                                                },
                                            }}
                                        />
                                        {sortable?.handle}

                                        {canRemoveEntries && (
                                            <Box
                                                sx={{
                                                    ml: 'auto',
                                                    cursor: isDisabled ? 'not-allowed' : undefined,
                                                }}
                                            >
                                                <Button
                                                    color="error"
                                                    variant="text"
                                                    size="small"
                                                    startIcon={<Delete sx={{fontSize: 16}}/>}
                                                    onClick={() => {
                                                        setConfirmDelete(() => () => handleDelete(val, valueIndex));
                                                    }}
                                                    disabled={isDisabled}
                                                    sx={{
                                                        minHeight: 24,
                                                        px: 0.75,
                                                        py: 0.25,
                                                        fontSize: '0.75rem',
                                                        lineHeight: 1.2,
                                                        '& .MuiButton-startIcon': {
                                                            mr: 0.5,
                                                        },
                                                    }}
                                                >
                                                    {stringOrDefault(element.removeLabel, 'Datensatz löschen')}
                                                </Button>
                                            </Box>
                                        )}
                                    </Box>
                                    <Grid
                                        container
                                        spacing={2}
                                        sx={{mt: 0}}
                                    >
                                        <ReplicatingContainerRowScope
                                            containerId={element.id}
                                            authoredElementValues={rowValues}
                                            derivedData={rowDerivedData}
                                        >
                                            {(children ?? []).map((child) => (
                                                <ViewDispatcherComponent
                                                    {...props}
                                                    key={child.id}
                                                    element={child}
                                                    isBusy={isDisabled}
                                                    authoredElementValues={rowValues}
                                                    derivedData={rowDerivedData}
                                                    onAuthoredElementValuesChange={(data, triggeringElementIds) => {
                                                        const newValue = (value ?? [])
                                                            .map((v, i) => i === valueIndex ? updateReplicatingContainerElementValues(v, data) : v);
                                                        setValue(newValue, triggeringElementIds);
                                                    }}
                                                    onElementBlur={(data, triggeringElementIds) => {
                                                        const newValue = (value ?? [])
                                                            .map((v, i) => i === valueIndex ? updateReplicatingContainerElementValues(v, data) : v);
                                                        onBlur(newValue, triggeringElementIds);
                                                    }}
                                                />
                                            ))}
                                        </ReplicatingContainerRowScope>
                                    </Grid>
                                </Box>
                            );

                            return isSortable ? (
                                <SortableRow
                                    key={rowKey}
                                    dragType={rowDragType}
                                    index={valueIndex}
                                    rowCount={value?.length ?? 0}
                                    itemLabel={itemLabel}
                                    onMove={handleMove}
                                >
                                    {renderRow}
                                </SortableRow>
                            ) : renderRow();
                        })}

                        {shouldShowEmptyState && (
                            <Box
                                sx={{
                                    px: {xs: 2, sm: 2.5},
                                    py: 2.5,
                                    textAlign: 'left',
                                }}
                            >
                                <Typography variant="body2" color="text.secondary">
                                    Keine Datensätze vorhanden.{' '}
                                    {displayedMinimumSets > 0 && (
                                        <>
                                            {element.required ? 'Mindestens' : 'Bei Eingabe mindestens'} {displayedMinimumSets} {displayedMinimumSets === 1 ? 'Datensatz' : 'Datensätze'} erforderlich.
                                        </>
                                    )}
                                </Typography>
                            </Box>
                        )}

                        <Box
                            sx={{
                                px: 1.5,
                                py: 0.75,
                                borderTop: '1px solid',
                                borderColor: 'divider',
                                backgroundColor: 'action.hover',
                                cursor: isAddDisabled ? 'not-allowed' : undefined,
                            }}
                        >
                            <Button
                                startIcon={<AddCircleOutlineOutlinedIcon/>}
                                onClick={handleAdd}
                                size="small"
                                disabled={isAddDisabled}
                            >
                                {stringOrDefault(element.addLabel, 'Datensatz hinzufügen')}
                            </Button>
                        </Box>
                    </Box>
                )}
            </FormFieldGroup>

            <ConfirmDialog
                isDestructive
                title="Möchten Sie diesen Datensatz wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei
                Bedarf erneut eingeben. Möchten Sie den Datensatz wirklich löschen?
            </ConfirmDialog>
        </>
    );
}

interface ReplicatingContainerRowScopeProps extends ViewDispatcherRowScope {
    children: ReactNode;
}

/**
 * Lets the fields of a row read the values of this row and of the rows around it, e.g. a dependent select whose
 * referenced select belongs to a surrounding row.
 */
function ReplicatingContainerRowScope(props: ReplicatingContainerRowScopeProps) {
    const {
        children,
        ...rowScope
    } = props;
    const context = useContext(ViewDispatcherContext);

    if (context == null) {
        return <>{children}</>;
    }

    return (
        <ViewDispatcherContextProvider
            value={{
                ...context,
                rowScopes: [...(context.rowScopes ?? []), rowScope],
            }}
        >
            {children}
        </ViewDispatcherContextProvider>
    );
}

interface SortableRowSlots {
    rowRef: RefObject<HTMLDivElement | null>;
    handle: ReactNode;
    isDragging: boolean;
    dropEdge: 'top' | 'bottom' | undefined;
}

interface SortableRowDragItem {
    index: number;
}

interface SortableRowProps {
    dragType: string;
    index: number;
    rowCount: number;
    itemLabel: string;
    onMove: (fromIndex: number, toIndex: number) => void;
    children: (sortable: SortableRowSlots) => ReactNode;
}

/**
 * Makes a row draggable by its handle and a drop target for the other rows of the same container. The handle also
 * opens a menu that moves the row by one position, which works with keyboard and touch input, where dragging does not.
 */
function SortableRow(props: SortableRowProps) {
    const {
        dragType,
        index,
        rowCount,
        itemLabel,
        onMove,
        children,
    } = props;

    const rowRef = useRef<HTMLDivElement | null>(null);
    const handleId = useId();
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);

    const [{isDragging}, drag, preview] = useDrag<SortableRowDragItem, void, {isDragging: boolean}>(() => ({
        type: dragType,
        item: {index},
        collect: (monitor) => ({
            isDragging: monitor.isDragging(),
        }),
    }), [dragType, index]);

    const [{sourceIndex}, drop] = useDrop<SortableRowDragItem, void, {sourceIndex: number | undefined}>(() => ({
        accept: dragType,
        drop: (item) => onMove(item.index, index),
        collect: (monitor) => ({
            sourceIndex: monitor.isOver() ? monitor.getItem()?.index : undefined,
        }),
    }), [dragType, index, onMove]);

    preview(drop(rowRef));

    const dropEdge = sourceIndex == null || sourceIndex === index ? undefined : sourceIndex < index ? 'bottom' : 'top';

    const handleMenuMove = (toIndex: number) => {
        setMenuAnchor(null);
        onMove(index, toIndex);
    };

    const handle = (
        <>
            <IconButton
                id={handleId}
                // Firefox does not reliably start drags from native button elements, so the handle renders as a
                // span, which MUI still exposes and operates as a button.
                component="span"
                ref={(node: HTMLElement | null) => {
                    drag(node);
                }}
                aria-label={`${itemLabel} verschieben`}
                aria-haspopup="menu"
                aria-expanded={menuAnchor != null ? true : undefined}
                onClick={(event) => setMenuAnchor(event.currentTarget)}
                sx={{
                    p: 0.25,
                    ml: 0.5,
                    color: 'text.secondary',
                    cursor: 'grab',
                    opacity: menuAnchor != null ? 1 : 0,
                    transition: (theme: Theme) => theme.transitions.create('opacity'),
                    // Only the handle of the hovered or focused row appears, not those of nested containers.
                    '[data-replicating-container-row]:hover > * > &, [data-replicating-container-row]:focus-within > * > &': {
                        opacity: 1,
                    },
                    '@media (hover: none)': {
                        opacity: 1,
                    },
                }}
            >
                <DragIndicator fontSize="small"/>
            </IconButton>
            <Menu
                anchorEl={menuAnchor}
                open={menuAnchor != null}
                onClose={() => setMenuAnchor(null)}
                slotProps={{list: {'aria-labelledby': handleId}}}
            >
                <MenuItem
                    disabled={index === 0}
                    onClick={() => handleMenuMove(index - 1)}
                >
                    Nach oben verschieben
                </MenuItem>
                <MenuItem
                    disabled={index >= rowCount - 1}
                    onClick={() => handleMenuMove(index + 1)}
                >
                    Nach unten verschieben
                </MenuItem>
            </Menu>
        </>
    );

    return (
        <>
            {children({
                rowRef,
                handle,
                isDragging,
                dropEdge,
            })}
        </>
    );
}

interface DatasetSkeletonProps {
    size: number | null | undefined;
}

function DatasetSkeleton(props: DatasetSkeletonProps) {
    const {
        size,
    } = props;

    const fields = useMemo(() => {
        const res = [];

        for (let i = 0; i < (size ?? 2); i++) {
            res.push(
                <Grid
                    key={i}
                    size={12}
                >
                    <Skeleton
                        variant="rectangular"
                        height={56}
                        sx={{
                            borderRadius: '4px',
                        }}
                    />
                </Grid>,
            );
        }

        return res;
    }, [size]);

    return (
        <Box
            sx={{
                px: {xs: 2, sm: 2.5},
                pt: 2,
                pb: 2.5,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    rowGap: '10px',
                }}
            >
                <Skeleton
                    variant="text"
                    width={88}
                    height={24}
                />
                <Skeleton
                    variant="rectangular"
                    width={132}
                    height={24}
                    sx={{ml: 'auto'}}
                />
            </Box>
            <Grid
                container
                spacing={2}
                sx={{mt: 0}}
            >
                {fields}
            </Grid>
        </Box>
    );
}
