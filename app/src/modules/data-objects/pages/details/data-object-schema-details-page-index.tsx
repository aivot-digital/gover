import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {isAdmin} from '../../../../utils/is-admin';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {useConfirm} from '../../../../providers/confirm-provider';
import {DataObjectSchema, ID_GEN_CUSTOM, ID_GEN_SERIAL, ID_GEN_UUID} from '../../models/data-object-schema';
import {ElementTreeTree} from '../../../../components/element-tree/element-tree-tree';
import {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {ElementType} from '../../../../data/element-type/element-type';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {RadioFieldComponent} from '../../../../components/radio-field/radio-field-component';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useConfirmDialog} from '../../../../hooks/use-confirm-dialog';
import {ConfirmDialogV2} from '../../../../dialogs/confirm-dialog/confirm-dialog-v2';

export const _YupSchema = {
    key: yup.string()
        .trim()
        .min(3, 'Der Schlüssel des Datenobjektschemas muss mindestens 3 Zeichen lang sein.')
        .max(64, 'Der Schlüssel des Datenobjektschemas darf maximal 64 Zeichen lang sein.')
        .matches(/^[a-zA-Z][a-zA-Z0-9_]{2,}$/, 'Der Schlüssel darf nur alphanumerische Zeichen und Unterstriche (_) enthalten und muss mit einem Buchstaben beginnen.')
        .required('Der Schlüssel des Datenobjektschemas ist ein Pflichtfeld.'),
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Datenobjektschemas muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Datenobjektschemas darf maximal 255 Zeichen lang sein.')
        .required('Der Name des Datenobjektschemas ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(10, 'Die Beschreibung muss mindestens 10 Zeichen lang sein.')
        .max(500, 'Die Beschreibung darf maximal 500 Zeichen lang sein.')
        .required('Die Beschreibung des Datenobjektschemas ist ein Pflichtfeld.'),
    idGen: yup.string()
        .trim()
        .max(64, 'Die ID Formatvorlage darf maximal 64 Zeichen lang sein.')
        .required('Die Angabe des ID Typs ist ein Pflichtfeld.'),
};

export function DataObjectSchemaDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const api = useApi();

    const {
        item: originalDataObject,
        setItem,
        isNewItem,
        isBusy,
        setIsBusy,
    } = useContext<GenericDetailsPageContextType<DataObjectSchema, void>>(GenericDetailsPageContext);

    const {
        currentItem: currentDataObject,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<DataObjectSchema>(originalDataObject, yup.object(_YupSchema) as any);

    const changeBlocker = useChangeBlocker(originalDataObject, currentDataObject);

    const {
        confirmOptions: confirmDeleteOptions,
        showConfirmDialog: showConfirmDeleteDialog,
        hideConfirmDialog: hideConfirmDeleteDialog,
    } = useConfirmDialog();

    if (currentDataObject == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (currentDataObject == null) {
            return;
        }

        if (isBusy) {
            return;
        }

        setIsBusy(true);

        if (isNewItem) {
            new DataObjectSchemasApiService(api)
                .create(currentDataObject)
                .then(newDataObjectSchema => {
                    setItem(newDataObjectSchema);
                    reset();

                    dispatch(showSuccessSnackbar('Neues Datenobjektschema erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/data-objects/${newDataObjectSchema.key}`, {replace: true});
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
            new DataObjectSchemasApiService(api)
                .update(currentDataObject.key, currentDataObject)
                .then(updatedDataObjectSchema => {
                    setItem(updatedDataObjectSchema);
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen am Datenobjektschema erfolgreich gespeichert.'));
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

    const handleDelete = () => {
        showConfirmDeleteDialog({
            title: 'Datenobjektschema löschen',
            state: {},
            onRender: (state, updateState) => {
                return (
                    <Typography>
                        Möchten Sie das Datenobjektschema wirklich löschen?
                        Alle Datenobjekte, die diesem Schema zugeordnet sind, werden ebenfalls gelöscht.
                        Dieser Vorgang kann nicht rückgängig gemacht werden.
                    </Typography>
                );
            },
            onConfirm: (state) => {
                if (originalDataObject == null || isNewItem) {
                    return;
                }

                setIsBusy(true);

                new DataObjectSchemasApiService(api)
                    .destroy(originalDataObject.key)
                    .then(() => {
                        reset(); // prevent change blocker by resetting unsaved changes
                        navigate('/data-objects', {
                            replace: true,
                        });
                        dispatch(showSuccessSnackbar('Das Datenobjektschema wurde erfolgreich gelöscht.'));
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Beim Löschen des Datenobjektschemas ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.'));
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
            <TextFieldComponent
                label="Eindeutiger Bezeichner"
                required
                value={currentDataObject.key}
                onChange={handleInputChange('key')}
                onBlur={handleInputBlur('key')}
                disabled={!isNewItem || isBusy || !userIsAdmin}
                error={errors.key}
                maxCharacters={64}
                minCharacters={3}
                hint="Dient dem Zugriff durch auf die Objekte dieses Datenobjektschemas. Der Schlüssel muss eindeutig sein und darf nur alphanumerische Zeichen und Unterstriche (_) enthalten. Der Schlüssel darf nicht mit einer Zahl beginnen. Der Schlüssel kann nicht geändert werden, nachdem das Datenobjektschema erstellt wurde."
                pattern={{
                    regex: '^[a-z_][a-z0-9_]+$',
                    message: 'Der Schlüssel darf nur kleine alphanumerische Zeichen und Unterstriche (_) enthalten und darf nicht mit einer Zahl beginnen.',
                }}
            />

            <TextFieldComponent
                label="Name"
                required
                value={currentDataObject.name}
                onChange={handleInputChange('name')}
                onBlur={handleInputBlur('name')}
                disabled={isBusy || !userIsAdmin}
                error={errors.name}
                minCharacters={3}
                maxCharacters={255}
                hint="Dient der internen Identifizierung des Zahlungsdienstleisters."
            />

            <TextFieldComponent
                label="Interne Beschreibung"
                required
                value={currentDataObject.description}
                onChange={handleInputChange('description')}
                onBlur={handleInputBlur('description')}
                multiline={true}
                disabled={isBusy || !userIsAdmin}
                error={errors.description}
                minCharacters={10}
                maxCharacters={500}
                hint="Interne Beschreibung des Zahlungsdienstleisters zur besseren Identifizierbarkeit. Sichtbar nur für Mitarbeiter:innen."
            />

            <RadioFieldComponent
                label="ID Typ"
                required
                value={(currentDataObject.idGen !== ID_GEN_UUID && currentDataObject.idGen !== ID_GEN_SERIAL && currentDataObject.idGen !== ID_GEN_CUSTOM) ? '' : currentDataObject.idGen}
                onChange={(val) => {
                    handleInputChange('idGen')(val ?? '');
                }}
                options={[
                    {label: 'UUID', subLabel: 'Eine automatisch generierte, eindeutige ID bestehend aus 36 Zeichen', value: ID_GEN_UUID},
                    {label: 'Seriell', subLabel: 'Eine aufsteigende, positive, ganze Zahl', value: ID_GEN_SERIAL},
                    {label: 'Selbstdefiniert', subLabel: 'Die ID muss beim Anlegen des Datenobjekt selbst definiert werden. Das Datenobjekt benötigt zwangsweise ein Feld mit der ID $id', value: ID_GEN_CUSTOM},
                    {label: 'Formatvorlage', subLabel: 'Die ID wird automatisch aus einer Formatvorlage erzeugt. Diese Vorlage muss zwingen am Anfang oder Ende einen Platzhalter für eine aufsteigende Nummer führen', value: ''},
                ]}
                disabled={isBusy || !userIsAdmin || !isNewItem}
            />

            {
                currentDataObject.idGen !== ID_GEN_UUID &&
                currentDataObject.idGen !== ID_GEN_SERIAL &&
                currentDataObject.idGen !== ID_GEN_CUSTOM &&
                <TextFieldComponent
                    label="ID Formatvorlage"
                    required
                    value={currentDataObject.idGen}
                    onChange={handleInputChange('idGen')}
                    onBlur={handleInputBlur('idGen')}
                    disabled={isBusy || !userIsAdmin || !isNewItem}
                    error={errors.idGen}
                    minCharacters={3}
                    maxCharacters={64}
                    hint="Die Formatvorlage für die Generierung der IDs. Diese wird verwendet, um eindeutige IDs für die Objekte dieses Datenobjektschemas zu generieren."
                />
            }

            <ElementTreeTree<GroupLayout>
                label="Datenobjektschema"
                hint="Das Datenobjektschema beschreibt die Struktur der Daten, die in diesem Datenobjekt gespeichert werden. Es definiert die Felder und deren Typen."
                entity={{} as any}
                value={currentDataObject.schema}
                onChange={handleInputChange('schema')}
                editable={true}
                scope="data_modelling"
                enabledIdentityProviderInfos={[]}
                limitElementTypes={[
                    ElementType.Container,
                    ElementType.ReplicatingContainer,
                    ElementType.Text,
                    ElementType.Number,
                    ElementType.Checkbox,
                    ElementType.Select,
                    ElementType.Radio,
                    ElementType.MultiCheckbox,
                ]}
            />

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
                        !isNewItem &&
                        <Button
                            variant="outlined"
                            onClick={handleDelete}
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

            {changeBlocker.dialog}

            <ConfirmDialogV2
                options={confirmDeleteOptions}
            />
        </Box>
    );
}