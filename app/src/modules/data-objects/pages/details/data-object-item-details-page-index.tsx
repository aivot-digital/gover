import {Box, Button} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate, useParams} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {isAdmin} from '../../../../utils/is-admin';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {useConfirm} from '../../../../providers/confirm-provider';
import {DataObjectSchema} from '../../models/data-object-schema';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {DataObjectItemsApiService} from '../../data-object-items-api-service';
import {ViewDispatcherComponent} from '../../../../components/view-dispatcher.component';
import {flattenElements} from '../../../../utils/flatten-elements';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {DataObjectItem} from '../../models/data-object-item';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';

export function DataObjectItemDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);
    const showConfirm = useConfirm();

    const dataObjectKey = useParams().schemaKey;
    const api = useApi();

    const [dataObject, setDataObject] = useState<DataObjectSchema>();
    useEffect(() => {
        if (dataObjectKey == null) {
            return;
        }

        new DataObjectSchemasApiService(api)
            .retrieve(dataObjectKey)
            .then((dataObject) => {
                setDataObject(dataObject);
            })
            .catch((error) => {
                console.error('Error fetching data object:', error);
            });
    }, []);

    const {
        item: originalDataObjectItem,
        setItem,
        isNewItem,
        additionalData,
        isBusy,
        setIsBusy,
        setAdditionalData,
    } = useContext<GenericDetailsPageContextType<DataObjectItem, void>>(GenericDetailsPageContext);

    const {
        currentItem: currentDataObjectItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<DataObjectItem>(originalDataObjectItem, yup.object({}) as any);

    const changeBlocker = useChangeBlocker(originalDataObjectItem, currentDataObjectItem);

    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [relatedEntities, setRelatedEntities] = useState<ConstraintLinkProps[] | null>(null);

    const allElements = useMemo(() => {
        if (dataObject == null) {
            return [];
        }
        return flattenElements(dataObject.schema);
    }, [dataObject]);

    if (dataObject == null || currentDataObjectItem == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (currentDataObjectItem == null || dataObjectKey == null) {
            return;
        }

        if (isBusy) {
            return;
        }

        setIsBusy(true);

        if (isNewItem) {
            new DataObjectItemsApiService(api, dataObjectKey)
                .create(currentDataObjectItem)
                .then(newDataObjectItem => {
                    setItem(newDataObjectItem);
                    reset();

                    dispatch(showSuccessSnackbar('Neues Datenobjekt erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/data-objects/${newDataObjectItem.schemaKey}/items/${encodeURIComponent(newDataObjectItem.id)}`, {replace: true});
                    }, 0);
                })
                .catch(err => {
                    console.error(err);
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
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen am Datenobjekt erfolgreich gespeichert.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const checkAndHandleDelete = async () => {

    };

    const handleDelete = () => {

    };

    return (
        <Box>
            {
                !isNewItem &&
                <TextFieldComponent
                    label="Eindeutiger Bezeichner"
                    required
                    value={currentDataObjectItem.id}
                    onChange={handleInputChange('id')}
                    disabled={true}
                    hint="Dient dem Zugriff durch auf die Objekte dieses Datenobjektschemas. Der Schlüssel muss eindeutig sein und darf nur alphanumerische Zeichen und Unterstriche (_) enthalten. Der Schlüssel darf nicht mit einer Zahl beginnen. Der Schlüssel kann nicht geändert werden, nachdem das Datenobjektschema erstellt wurde."
                />
            }

            <ViewDispatcherComponent
                rootElement={dataObject.schema}
                allElements={allElements}
                element={dataObject.schema}
                isBusy={false}
                isDeriving={false}
                mode="viewer"
                elementData={currentDataObjectItem.data}
                onElementDataChange={handleInputChange('data')}
                derivationTriggerIdQueue={[]}
            />

            {changeBlocker.dialog}

            {
                userIsAdmin &&
                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 2,
                        gap: 2,
                    }}
                >
                    <Button
                        onClick={handleSave}
                        disabled={isBusy || hasNotChanged}
                        variant="contained"
                        color="primary"
                        startIcon={<SaveOutlinedIcon />}
                    >
                        Speichern
                    </Button>

                    {
                        isStringNotNullOrEmpty(currentDataObjectItem.id) &&
                        <Button
                            variant="outlined"
                            onClick={checkAndHandleDelete}
                            disabled={isBusy}
                            color="error"
                            sx={{
                                marginLeft: 'auto',
                            }}
                            startIcon={<DeleteOutlinedIcon />}
                        >
                            Löschen
                        </Button>
                    }
                </Box>
            }
        </Box>
    );
}