import React, {useCallback, useMemo, useState} from 'react';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Skeleton from '@mui/material/Skeleton';
import Typography from '@mui/material/Typography';
import {stringOrDefault} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import AddCircleOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AddCircle';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {flattenElements} from '../../utils/flatten-elements';
import {
    type ReplicatingContainerElementValue,
    type ReplicatingContainerElementValues,
    resolveReplicatingContainerElementValues,
    updateReplicatingContainerElementValues,
} from '../../models/element-data';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {resolveReplicatingContainerItemDerivedData} from '../../utils/element-data-utils';
import {ViewDispatcherComponent} from '../view-dispatcher/view-dispatcher.component';
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
        if (element.disabled) {
            return true;
        }

        if (isBusy) {
            return true;
        }

        if (isDeriving && hasDerivableAspects(element)) {
            return true;
        }

        return false;
    }, [element, isBusy, isDeriving]);

    const minRequiredSets = element.required === true ? (element.minimumRequiredSets ?? 1) : 0;
    const hasEntries = (value?.length ?? 0) > 0;
    const isAddDisabled = isDisabled || (element.maximumSets != null && element.maximumSets > 0 && (value ?? []).length >= element.maximumSets);
    const shouldShowEmptyState = !hasEntries;
    const isContainerBusy = isDeriving && hasDerivableAspects(element);
    const fieldError = errors != null && errors.length > 0 ? errors.join(' ') : undefined;

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

    return (
        <>
            <FormFieldGroup
                label={element.label ?? ''}
                hint={element.hint ?? undefined}
                error={fieldError}
                required={element.required ?? undefined}
                disabled={Boolean(element.disabled || isBusy)}
                busy={isContainerBusy}
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
                            const rowValues = resolveReplicatingContainerElementValues(val) ?? {};
                            const rowKey = val.id ?? valueIndex;
                            const itemLabelId = `${fieldContext.groupId}-item-${valueIndex}-label`;
                            const itemLabel = stringOrDefault(element.headlineTemplate, 'Datensatz #')
                                .replace('#', (valueIndex + 1).toFixed());

                            return derivationTriggerIdQueue.includes(`${element.id}.${valueIndex}`) ? (
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
                            ) : (
                                <Box
                                    key={rowKey}
                                    role="group"
                                    aria-labelledby={itemLabelId}
                                    sx={{
                                        px: {xs: 2, sm: 2.5},
                                        pt: 2,
                                        pb: 2.5,
                                        borderTop: valueIndex > 0 ? '1px solid' : undefined,
                                        borderColor: 'divider',
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

                                        {(minRequiredSets === 0 || valueIndex >= minRequiredSets) && (
                                            <Box
                                                sx={{
                                                    ml: 'auto',
                                                    cursor: isDisabled ? 'not-allowed' : undefined,
                                                }}
                                            >
                                                <Button
                                                    color="inherit"
                                                    variant="text"
                                                    size="small"
                                                    startIcon={<Delete sx={{fontSize: 16}}/>}
                                                    onClick={() => {
                                                        setConfirmDelete(() => () => handleDelete(val, valueIndex));
                                                    }}
                                                    disabled={isDisabled || (minRequiredSets > 0 && (value ?? []).length <= minRequiredSets)}
                                                    sx={{
                                                        minHeight: 24,
                                                        px: 0.75,
                                                        py: 0.25,
                                                        color: 'text.secondary',
                                                        fontSize: '0.75rem',
                                                        lineHeight: 1.2,
                                                        '& .MuiButton-startIcon': {
                                                            mr: 0.5,
                                                        },
                                                        '&:hover': {
                                                            color: 'error.main',
                                                            backgroundColor: 'action.hover',
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
                                        {(children ?? []).map((child) => (
                                            <ViewDispatcherComponent
                                                {...props}
                                                key={child.id}
                                                element={child}
                                                isBusy={isDisabled}
                                                authoredElementValues={rowValues}
                                                derivedData={resolveReplicatingContainerItemDerivedData(element, derivedData, valueIndex)}
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
                                    </Grid>
                                </Box>
                            );
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
                                    {minRequiredSets > 0 && (
                                        <>
                                            Mindestens {minRequiredSets} {minRequiredSets === 1 ? 'Datensatz ist' : 'Datensätze sind'} erforderlich.
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
