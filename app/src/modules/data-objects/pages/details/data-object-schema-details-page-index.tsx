import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useLocation, useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import * as yup from 'yup';
import {ObjectSchema} from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {DataObjectSchema, ID_GEN_CUSTOM, ID_GEN_SERIAL, ID_GEN_UUID} from '../../models/data-object-schema';
import {ElementTreeTree} from '../../../../components/element-tree/element-tree-tree';
import {GroupLayout} from '../../../../models/elements/form/layout/group-layout';
import {ElementType} from '../../../../data/element-type/element-type';
import {DataObjectSchemasApiService} from '../../data-object-schemas-api-service';
import {RadioFieldComponent} from '../../../../components/radio-field/radio-field-component';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useConfirm} from '../../../../providers/confirm-provider';
import {MultiCheckboxComponent, MultiCheckboxOptions} from '../../../../components/multi-checkbox-field/multi-checkbox-component';
import {flattenElements} from '../../../../utils/flatten-elements';
import {isAnyInputElement} from '../../../../models/elements/form/input/any-input-element';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {isApiError} from '../../../../models/api-error';
import {generateElementWithDefaultValues} from '../../../../utils/generate-element-with-default-values';
import {TextFieldElement} from '../../../../models/elements/form/input/text-field-element';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';

const ID_FIELD_ID = '$id';

const AllowedDisplayFieldTypes = [
    ElementType.Text,
    ElementType.Number,
    ElementType.Date,
    ElementType.Time,
    ElementType.Select,
    ElementType.Radio,
    ElementType.Checkbox,
    ElementType.MultiCheckbox,
];

export const YupSchema: ObjectSchema<Omit<DataObjectSchema, 'schema' | 'created' | 'updated' | 'displayFields'>> = yup.object({
    key: yup.string()
        .trim()
        .min(3, 'Der Schlüssel des Datenmodells muss mindestens 3 Zeichen lang sein.')
        .max(64, 'Der Schlüssel des Datenmodells darf maximal 64 Zeichen lang sein.')
        .matches(/^[a-zA-Z][a-zA-Z0-9_]{2,}$/, 'Der Schlüssel darf nur alphanumerische Zeichen und Unterstriche (_) enthalten und muss mit einem Buchstaben beginnen.')
        .required('Der Schlüssel des Datenmodells ist ein Pflichtfeld.'),
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Datenmodells muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Datenmodells darf maximal 255 Zeichen lang sein.')
        .required('Der Name des Datenmodells ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(10, 'Die Beschreibung muss mindestens 10 Zeichen lang sein.')
        .max(500, 'Die Beschreibung darf maximal 500 Zeichen lang sein.')
        .required('Die Beschreibung des Datenmodells ist ein Pflichtfeld.'),
    idGen: yup.string()
        .trim()
        .max(64, 'Die ID Formatvorlage darf maximal 64 Zeichen lang sein.')
        .required('Die Angabe des ID Typs ist ein Pflichtfeld.'),
});

const IdGenOptions = [
    {
        label: 'UUID',
        subLabel: 'Global eindeutige Kennung mit 36 Zeichen (z. B. 550e8400-e29b-41d4-a716-446655440000). Wird beim Anlegen erzeugt.',
        value: ID_GEN_UUID,
    },
    {
        label: 'Seriell fortlaufend',
        subLabel: 'Automatisch inkrementierte/hochgezählte ganze Zahl (1, 2, 3 …).',
        value: ID_GEN_SERIAL,
    },
    {
        label: 'Manuell festgelegt',
        subLabel: `Die ID wird manuell beim Anlegen eingetragen. Das Schema benötigt hierfür ein Pflichtfeld mit der Element-ID „${ID_FIELD_ID}“.`,
        value: ID_GEN_CUSTOM,
    },
    {
        label: 'Formatvorlage',
        subLabel: 'Erzeugt fortlaufende IDs nach einem vorgegeben Muster mit der Unterstützung von Platzhaltern (z. B. ANT-%Y-%M-%D-%I4 → ANT-2025-10-01-0001). ',
        value: '',
    },
];

export function DataObjectSchemaDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const location = useLocation();

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
        handleInputPatch,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<DataObjectSchema>(originalDataObject, YupSchema as any, true);

    const changeBlocker = useChangeBlocker(originalDataObject, currentDataObject, undefined, undefined, true);

    useEffect(() => {
        if (isNewItem && location.state != null) {
            handleInputPatch(location.state);
        }
    }, [isNewItem, location, originalDataObject]);

    const confirm = useConfirm();

    const availableDisplayFields: MultiCheckboxOptions[] = useMemo(() => {
        if (currentDataObject == null) {
            return [];
        }

        const elems = flattenElements(currentDataObject.schema, true);
        return elems
            .filter(e => isAnyInputElement(e) && AllowedDisplayFieldTypes.includes(e.type) && e.id !== ID_FIELD_ID)
            .map(e => ({
                label: generateComponentTitle(e),
                value: e.id,
            }));
    }, [currentDataObject]);

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

        const validationResult = validate();

        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        if (isNewItem) {
            new DataObjectSchemasApiService(api)
                .create(currentDataObject)
                .then(newDataObjectSchema => {
                    setItem(newDataObjectSchema);
                    reset();

                    dispatch(showSuccessSnackbar('Neues Datenmodell erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/data-models/${newDataObjectSchema.key}`, {replace: true});
                    }, 0);
                })
                .catch(err => {
                    console.error(err);
                    if (isApiError(err) && err.displayableToUser) {
                        dispatch(showErrorSnackbar(err.message));
                    } else {
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    }
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

                    dispatch(showSuccessSnackbar('Änderungen am Datenmodell erfolgreich gespeichert.'));
                })
                .catch(err => {
                    console.error(err);
                    if (isApiError(err) && err.displayableToUser) {
                        dispatch(showErrorSnackbar(err.message));
                    } else {
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    }
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const handleDelete = () => {
        if (originalDataObject == null || isNewItem) {
            return;
        }

        confirm({
            title: 'Datenmodell löschen',
            children: (
                <Typography>
                    Möchten Sie das Datenmodell wirklich löschen?
                    Alle Datenobjekte, die diesem Modell zugeordnet sind, werden ebenfalls gelöscht.
                    Dieser Vorgang kann nicht rückgängig gemacht werden.
                </Typography>
            ),
            confirmButtonText: 'Datenmodell endgültig löschen',
            confirmationText: originalDataObject.key,
            isDestructive: true,
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                setIsBusy(true);

                new DataObjectSchemasApiService(api)
                    .destroy(originalDataObject.key)
                    .then(() => {
                        reset(); // prevent change blocker by resetting unsaved changes
                        navigate('/data-models', {
                            replace: true,
                        });
                        dispatch(showSuccessSnackbar('Das Datenmodell wurde erfolgreich gelöscht.'));
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Beim Löschen des Datenmodells ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.'));
                        setIsBusy(false);
                    });
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
                disabled={!isNewItem || isBusy || !hasAccess}
                error={errors.key}
                maxCharacters={64}
                minCharacters={3}
                hint="Dient dem Zugriff auf die Objekte dieses Datenmodells. Der Schlüssel muss eindeutig sein und darf nur alphanumerische Zeichen und Unterstriche (_) enthalten. Der Schlüssel darf nicht mit einer Zahl beginnen. Der Schlüssel kann nicht geändert werden, nachdem das Datenmodell erstellt wurde."
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
                disabled={isBusy || !hasAccess}
                error={errors.name}
                minCharacters={3}
                maxCharacters={255}
                hint="Name des Datenmodells zur internen Identifizierung."
            />

            <TextFieldComponent
                label="Beschreibung"
                required
                value={currentDataObject.description}
                onChange={handleInputChange('description')}
                onBlur={handleInputBlur('description')}
                multiline={true}
                disabled={isBusy || !hasAccess}
                error={errors.description}
                minCharacters={10}
                maxCharacters={500}
                hint="Beschreibung des Datenmodells zum besseren Verständnis."
            />


            {
                !isNewItem &&
                <SelectFieldComponent
                    label="ID-Typ"
                    required
                    value={(currentDataObject.idGen !== ID_GEN_UUID && currentDataObject.idGen !== ID_GEN_SERIAL && currentDataObject.idGen !== ID_GEN_CUSTOM) ? '' : currentDataObject.idGen}
                    onChange={(val) => {
                    }}
                    options={IdGenOptions}
                    disabled={true}
                />
            }
            {
                isNewItem &&
                <RadioFieldComponent
                    label="ID-Typ"
                    required
                    value={(currentDataObject.idGen !== ID_GEN_UUID && currentDataObject.idGen !== ID_GEN_SERIAL && currentDataObject.idGen !== ID_GEN_CUSTOM) ? '' : currentDataObject.idGen}
                    onChange={(val) => {
                        if (val === ID_GEN_CUSTOM) {
                            const hasIdField = (currentDataObject?.schema.children ?? []).some(c => c.id === ID_FIELD_ID);
                            if (!hasIdField) {
                                handleInputPatch({
                                    idGen: ID_GEN_CUSTOM,
                                    schema: {
                                        ...currentDataObject.schema,
                                        children: [
                                            {
                                                ...generateElementWithDefaultValues(ElementType.Text),
                                                id: ID_FIELD_ID,
                                                name: 'ID',
                                                label: 'ID',
                                                hint: 'Eindeutige ID des Datenobjekts',
                                                required: true,
                                            } as TextFieldElement,
                                            ...currentDataObject.schema.children ?? [],
                                        ],
                                    },
                                });
                            }
                        } else {
                            handleInputChange('idGen')(val ?? '');
                        }
                    }}
                    options={IdGenOptions}
                    disabled={isBusy || !hasAccess || !isNewItem}
                />
            }

            {
                currentDataObject.idGen !== ID_GEN_UUID &&
                currentDataObject.idGen !== ID_GEN_SERIAL &&
                currentDataObject.idGen !== ID_GEN_CUSTOM &&
                <TextFieldComponent
                    label="ID-Formatvorlage"
                    required
                    value={currentDataObject.idGen}
                    onChange={handleInputChange('idGen')}
                    onBlur={handleInputBlur('idGen')}
                    disabled={isBusy || !hasAccess || !isNewItem}
                    error={errors.idGen}
                    minCharacters={3}
                    maxCharacters={64}
                    hint="Muster für automatisch generierte IDs. Unterstützte Platzhalter: %Y (Jahr), %M (Monat), %D (Tag), %I1–%I9 (fortlaufende Nummer mit führenden Nullen in gewählter Zahl). Der Platzhalter %I… muss zwingend enthalten sein und am Anfang ODER am Ende stehen (z. B. %Y-%M-%D-%I4)."
                />
            }

            <Box sx={{my: 3}}>
                <ElementTreeTree<GroupLayout>
                    label="Datenschema"
                    hint="Das Datenschema beschreibt die Struktur der Daten, die in den Datenobjekten gespeichert werden. Es definiert die Felder und deren Typen."
                    entity={currentDataObject.schema as any}
                    value={currentDataObject.schema}
                    onChange={handleInputChange('schema')}
                    editable={true}
                    scope="data_modelling"
                    enabledIdentityProviderInfos={[]}
                    limitElementTypes={[
                        ElementType.GroupLayout,
                        ElementType.ReplicatingContainer,
                        ElementType.Text,
                        ElementType.Number,
                        ElementType.Checkbox,
                        ElementType.Select,
                        ElementType.Radio,
                        ElementType.MultiCheckbox,
                        ElementType.Table,
                        ElementType.Date,
                        ElementType.Time,
                    ]}
                />

                {
                    availableDisplayFields.length > 0 &&
                    <MultiCheckboxComponent
                        label="Anzeigeattribute"
                        hint="Die Werte dieser Felder bzw. Attribute werden in Listenansichten zur Identifizierung angezeigt."
                        value={currentDataObject.displayFields ?? []}
                        onChange={(val) => {
                            handleInputChange('displayFields')(val ?? []);
                        }}
                        options={availableDisplayFields}
                        displayInline
                    />
                }
            </Box>

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 2,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !hasAccess}
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
                        disabled={isBusy || !hasAccess}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<Delete />}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlocker.dialog}
        </Box>
    );
}