import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {Box, Button, FormHelperText, FormLabel, Grid, Skeleton, Typography} from '@mui/material';
import {stringOrDefault} from '../../utils/string-utils';
import {generateElementIdForReplicatingContainerChild} from '../../utils/id-utils';
import {type BaseViewProps} from '../../views/base-view';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {flattenElements} from '../../utils/flatten-elements';
import {useAppSelector} from '../../hooks/use-app-selector';
import {enqueueDerivationTriggerId, selectFunctionReferences} from '../../slices/app-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {FunctionType} from '../../utils/function-status-utils';

export function ReplicatingContainerView(props: BaseViewProps<ReplicatingContainerLayout, string[]>) {
    const dispatch = useAppDispatch();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const references = useAppSelector(selectFunctionReferences);
    const derivationTriggerIdQueue = useAppSelector((state) => state.app.derivationTriggerIdQueue);

    const {
        element,
        value,
        setValue,
        allElements,
        isDeriving,
        isBusy,
    } = props;

    const {
        children,
    } = element;

    const someChildWithAReferenceExists = useMemo(() => {
        if (references == null) {
            return false;
        }

        const flattenedChildren = [];
        for (const child of children ?? []) {
            const flattened = flattenElements(child, false);
            flattenedChildren.push(...flattened);
        }

        for (const child of flattenedChildren) {
            const childReferences = references
                .filter(ref => (
                    ref.source.id === child.id &&
                    ref.isSameStep &&
                    ref.functionType !== FunctionType.VALIDATION
                ));

            if (childReferences.length > 0) {
                return true;
            }
        }

        return false;
    }, [children, references]);

    const someChildWithDerivableAspectsExists = useMemo(() => {
        const flattenedChildren = [];
        for (const child of children ?? []) {
            const flattened = flattenElements(child, false);
            flattenedChildren.push(...flattened);
        }

        for (const child of flattenedChildren) {
            if (hasDerivableAspects(child)) {
                return true;
            }
        }

        return false;
    }, [children]);

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
    }, [element, isBusy, isDeriving]);

    const minRequiredSets = element.required === true ? (element.minimumRequiredSets ?? 1) : 0;

    useEffect(() => {
        if (minRequiredSets > 0 && (value == null || value.length < minRequiredSets)) {
            const forcedChildren = Array.from({length: minRequiredSets}, () => generateElementIdForReplicatingContainerChild());

            setValue(forcedChildren);

            if (someChildWithAReferenceExists || someChildWithDerivableAspectsExists) {
                dispatch(enqueueDerivationTriggerId(forcedChildren[0]));
            }
        }
    }, [setValue, value, element]);

    const handleAdd = useCallback(() => {
        const newChildId = generateElementIdForReplicatingContainerChild();
        // Keep this order of operations to guarantee that listeners consuming the derivation queue have the updated customer input in place
        setValue([
            ...(value ?? []),
            newChildId,
        ]);
        // Check if any child has references to trigger the derivation queue
        if (someChildWithAReferenceExists || someChildWithDerivableAspectsExists) {
            dispatch(enqueueDerivationTriggerId(newChildId));
        }
    }, [element, setValue, value, someChildWithAReferenceExists]);

    const handleDelete = useCallback((id: string) => {
        const newValue = (value ?? [])
            .filter((val: string) => val !== id);
        setValue(newValue.length === 0 ? undefined : newValue);
        setConfirmDelete(undefined);
    }, [setValue, value]);

    return (
        <Box sx={{mt: 2}}>
            {
                (element.label != null) &&
                <FormLabel>
                    {element.label}
                </FormLabel>
            }

            {
                (element.hint != null) &&
                <FormHelperText>
                    {element.hint}
                </FormHelperText>
            }

            {
                value?.map((val: string, valueIndex: number) => derivationTriggerIdQueue.includes(val) ? (
                    // Skeleton
                    <Box
                        key={val}
                        sx={{
                            my: 2,
                            px: 3,
                            pt: 2.4,
                            pb: 3.4,
                            border: '1px solid #D4D4D4',
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
                                width="30%"
                                height={32}
                            />
                            <Skeleton
                                variant="rectangular"
                                width={170}
                                height={36}
                                sx={{ml: 'auto'}}
                            />
                        </Box>
                        <Grid
                            container
                            spacing={2}
                            sx={{mt: 0}}
                        >
                            {
                                Array.from({length: (element.children?.length ?? 2)}).map((_, i) => (
                                    <Grid
                                        item
                                        xs={12}
                                        key={i}
                                    >
                                        <Skeleton
                                            variant="rectangular"
                                            height={56}
                                            sx={{borderRadius: '4px'}}
                                        />
                                    </Grid>
                                ))
                            }
                        </Grid>
                    </Box>
                ) : (
                    <Box
                        key={val}
                        sx={{
                            my: 2,
                            px: 3,
                            py: 2.4,
                            border: '1px solid #D4D4D4',
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
                            <Typography
                                component={'p'}
                                variant="h6"
                                sx={{fontSize: '1.125rem'}}
                            >
                                {
                                    (element.headlineTemplate ?? '').replace('#', (valueIndex + 1).toFixed())
                                }
                            </Typography>

                            {
                                (element.disabled !== true) &&
                                (minRequiredSets === 0 || valueIndex >= minRequiredSets) &&
                                <Box
                                    sx={{
                                        ml: 'auto',
                                    }}
                                >
                                    <Button
                                        color="error"
                                        size="small"
                                        endIcon={
                                            <DeleteForeverOutlinedIcon
                                                sx={{
                                                    marginTop: '-4px',
                                                }}
                                            />
                                        }
                                        onClick={() => {
                                            setConfirmDelete(() => () => handleDelete(val));
                                        }}
                                        disabled={isDisabled || (minRequiredSets > 0 && (value ?? []).length <= minRequiredSets)}
                                    >
                                        {
                                            stringOrDefault(element.removeLabel, 'Datensatz löschen')
                                        }
                                    </Button>
                                </Box>
                            }
                        </Box>
                        <Grid
                            container
                            spacing={2}
                            sx={{mt: 0}}
                        >
                            {
                                (element.children ?? []).map((child, childIndex) => (
                                    <ViewDispatcherComponent
                                        allElements={allElements}
                                        key={childIndex}
                                        element={child}
                                        idPrefix={`${element.id}_${val}_`}
                                        isBusy={isBusy}
                                        isDeriving={props.isDeriving}
                                        valueOverride={props.valueOverride}
                                        errorsOverride={props.errorsOverride}
                                    />
                                ))
                            }
                        </Grid>
                    </Box>
                ))
            }
            {
                (element.disabled !== true) &&
                <div>
                    <Button
                        startIcon={<AddCircleOutlineOutlinedIcon
                            sx={{marginTop: '-2px'}}
                        />}
                        sx={{
                            mt: 2,
                            mb: 3,
                        }}
                        onClick={handleAdd}
                        variant={'outlined'}
                        disabled={isDisabled || (element.maximumSets != null && element.maximumSets > 0 && (value ?? []).length >= element.maximumSets)}
                    >
                        {
                            stringOrDefault(element.addLabel, 'Datensatz hinzufügen')
                        }
                    </Button>
                </div>
            }

            <ConfirmDialog
                title="Möchten Sie diesen Datensatz wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben. Möchten Sie den Datensatz wirklich löschen?
            </ConfirmDialog>
        </Box>
    );
}

