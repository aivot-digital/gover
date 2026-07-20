import * as yup from 'yup';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType,
} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate, useParams} from 'react-router-dom';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker-2';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {DataObjectSchema} from '../../models/data-object-schema';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {DataObjectItem} from '../../models/data-object-item';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ConfirmDialogV2} from '../../../../dialogs/confirm-dialog/confirm-dialog-v2';
import {useConfirmDialog} from '../../../../hooks/use-confirm-dialog';
import {applyYupErrorsToElementData, goverSchemaToYup} from '../../../../utils/gover-schema-to-yup';
import Grid from '@mui/material/Grid';
import {format as formatDateTime} from 'date-fns/format';
import {isApiError} from '../../../../models/api-error';
import {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {createDerivedRuntimeElementData, isDerivedRuntimeElementData} from '../../../../models/element-data';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

export function DataObjectItemDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const canDeleteDataObjectItem = useHasSystemPermission(Permission.OBJECT_ITEM_DELETE);

    const dataObjectKey = useParams().schemaKey;
    const api = useApi();

    const [dataObjectSchema, setDataObjectSchema] = useState<DataObjectSchema>();
    const [derivedData, setDerivedData] = useState(createDerivedRuntimeElementData());

    useEffect(() => {
        if (dataObjectKey == null) {
            return;
        }

        new DataObjectSchemasApiService(api)
            .retrieve(dataObjectKey)
            .then((dataObject) => {
                setDataObjectSchema(dataObject);
            })
            .catch((error) => {
                console.error('Error fetching data object:', error);
            });
    }, []);

    const yupSchema = useMemo(() => {
        if (dataObjectSchema == null || derivedData == null) {
            return yup
                .object()
                .required()
                .shape({});
        }

        return yup
            .object()
            .required()
            .shape({
                data: yup
                    .object()
                    .required()
                    .shape(goverSchemaToYup(dataObjectSchema.schema, derivedData.elementStates)),
            });
    }, [dataObjectSchema, derivedData.elementStates]);

    const {
        item: originalDataObjectItem,
        setItem,
        isNewItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext<GenericDetailsPageContextType<DataObjectItem, void>>(GenericDetailsPageContext);

    const {
        currentItem: currentDataObjectItem,
        errors,
        hasNotChanged,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<DataObjectItem>(originalDataObjectItem, yupSchema as any, true);

    const changeBlocker = useChangeBlocker({
        original: originalDataObjectItem,
        edited: currentDataObjectItem,
        useDeepEquals: true,
    });

    const {
        confirmOptions: confirmDeleteOptions,
        showConfirmDialog: showConfirmDeleteDialog,
        hideConfirmDialog: hideConfirmDeleteDialog,
    } = useConfirmDialog();
    const editPermission = isNewItem ? Permission.OBJECT_ITEM_CREATE : Permission.OBJECT_ITEM_UPDATE;
    const editDisabledTooltip = !isEditable
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledTooltip = !canDeleteDataObjectItem
        ? formatMissingPermissionTooltip(Permission.OBJECT_ITEM_DELETE)
        : undefined;

    useEffect(() => {
        if (errors == null || Object.keys(errors).length === 0 || currentDataObjectItem == null || dataObjectSchema == null) {
            return;
        }

        setDerivedData((currentDerivedData) => applyYupErrorsToElementData(
            dataObjectSchema.schema,
            currentDataObjectItem.data,
            errors,
            currentDerivedData,
        ));
    }, [errors, currentDataObjectItem, dataObjectSchema]);

    const schema = useMemo(() => {
        if (dataObjectSchema == null) {
            return null;
        }

        if (isNewItem || dataObjectSchema.idGen !== '__CUSTOM__') {
            return dataObjectSchema.schema;
        }

        return {
            ...dataObjectSchema.schema,
            children: dataObjectSchema.schema.children.filter(c => c.id !== '$id'),
        };
    }, [isNewItem, dataObjectSchema]);

    if (dataObjectSchema == null || currentDataObjectItem == null || schema == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleSave = () => {
        if (currentDataObjectItem == null || dataObjectKey == null) {
            return;
        }

        if (isBusy || !isEditable) {
            return;
        }

        const validationResult = validate();

        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        if (isNewItem) {
            new DataObjectItemsApiService(api, dataObjectKey)
                .create(currentDataObjectItem)
                .then(newDataObjectItem => {
                    setItem(newDataObjectItem);
                    setDerivedData(createDerivedRuntimeElementData());
                    reset();

                    dispatch(showSuccessSnackbar('Neues Datenobjekt erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/data-objects/${newDataObjectItem.schemaKey}/${encodeURIComponent(newDataObjectItem.id)}`, {replace: true});
                    }, 0);
                })
                .catch(err => {
                    if (isApiError(err) && err.status === 400 && typeof err.details === 'object' && isDerivedRuntimeElementData(err.details)) {
                        setDerivedData(err.details);
                    } else {
                        console.error(err);
                    }

                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            new DataObjectItemsApiService(api, dataObjectKey)
                .update(currentDataObjectItem.id, currentDataObjectItem)
                .then(updatedDataObjectItem => {
                    setItem(updatedDataObjectItem);
                    setDerivedData(createDerivedRuntimeElementData());
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen am Datenobjekt erfolgreich gespeichert.'));
                })
                .catch(err => {
                    if (err.status === 400 && 'details' in err && isDerivedRuntimeElementData(err.details)) {
                        setDerivedData(err.details);
                    } else {
                        console.error(err);
                    }

                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const handleDelete = () => {
        if (!canDeleteDataObjectItem) {
            return;
        }

        showConfirmDeleteDialog({
            title: 'Datenobjekt löschen',
            state: {},
            onRender: (state, updateState) => {
                return (
                    <Typography>
                        Möchten Sie das Datenobjekt wirklich löschen?
                        Dieser Vorgang kann nicht rückgängig gemacht werden.
                    </Typography>
                );
            },
            onConfirm: (state) => {
                if (originalDataObjectItem == null || isNewItem || dataObjectKey == null) {
                    return;
                }

                setIsBusy(true);

                new DataObjectItemsApiService(api, dataObjectKey)
                    .destroy(originalDataObjectItem.id)
                    .then(() => {
                        reset(); // prevent change blocker by resetting unsaved changes
                        navigate(`/data-objects/${dataObjectKey}/`, {
                            replace: true,
                        });
                        dispatch(showSuccessSnackbar('Das Datenobjekt wurde erfolgreich gelöscht.'));
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Beim Löschen des Datenobjektes ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.'));
                        setIsBusy(false);
                    });
            },
            onCancel: () => {
                hideConfirmDeleteDialog();
            },
        });
    };

    return (
        <Box>
            {
                !isNewItem &&
                <Grid
                    container
                    columnSpacing={2}
                    sx={{
                        mb: 2,
                    }}
                >
                    <Grid
                        size={{
                            xs: 12,
                        }}
                    >
                        <TextFieldComponent
                            required
                            label="Eindeutiger Bezeichner"
                            value={currentDataObjectItem.id}
                            onChange={handleInputChange('id')}
                            disabled={true}
                            hint="Dient dem Zugriff auf dieses Datenobjekt. Der Schlüssel muss eindeutig sein und darf nur alphanumerische Zeichen und Unterstriche (_) enthalten. Der Schlüssel darf nicht mit einer Zahl beginnen. Der Schlüssel kann nicht geändert werden, nachdem das Datenobjekt erstellt wurde."
                        />
                    </Grid>

                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Erstellt am"
                            value={formatDateTime(currentDataObjectItem.created, 'dd.MM.yyyy HH:mm:ss') + ' Uhr'}
                            onChange={() => {
                            }}
                            disabled={true}
                        />
                    </Grid>

                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Geändert am"
                            value={formatDateTime(currentDataObjectItem.updated, 'dd.MM.yyyy HH:mm:ss') + ' Uhr'}
                            onChange={() => {
                            }}
                            disabled={true}
                        />
                    </Grid>
                </Grid>
            }

            <ElementDerivationContext
                element={schema}
                authoredElementValues={currentDataObjectItem.data}
                derivedData={derivedData}
                onAuthoredElementValuesChange={(changedElementData) => {
                    handleInputChange('data')(changedElementData);
                }}
                onDerivedDataChange={setDerivedData}
                disabled={!isEditable}
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 2,
                    gap: 2,
                }}
            >
                <DisabledTooltip
                    title={editDisabledTooltip}
                    disabled={isBusy || hasNotChanged || !isEditable}
                >
                    <Button
                        onClick={handleSave}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        variant="contained"
                        color="primary"
                        startIcon={<SaveOutlinedIcon/>}
                    >
                        Speichern
                    </Button>
                </DisabledTooltip>

                {
                    !isNewItem &&
                    <DisabledTooltip
                        title={deleteDisabledTooltip}
                        disabled={isBusy || !canDeleteDataObjectItem}
                        wrapperSx={{marginLeft: 'auto'}}
                    >
                        <Button
                            variant="outlined"
                            onClick={handleDelete}
                            disabled={isBusy || !canDeleteDataObjectItem}
                            color="error"
                            startIcon={<Delete/>}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialogV2
                options={confirmDeleteOptions}
            />
        </Box>
    );
}
