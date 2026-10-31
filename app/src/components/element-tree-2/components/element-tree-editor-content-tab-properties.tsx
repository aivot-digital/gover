import {ElementType} from '../../../data/element-type/element-type';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeEditorContext} from './element-tree-editor-context';
import React, {useEffect, useMemo} from 'react';
import {ElementEditorSectionHeader} from '../../element-editor-section-header/element-editor-section-header';
import {Grid, Typography} from '@mui/material';
import {TextFieldComponent} from '../../text-field/text-field-component';
import ContentPasteIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useElementTreeContext} from '../element-tree-context';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {isAnyFormElement} from '../../../models/elements/form/any-form-element';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {editors} from '../../../editors';
import {EditorDispatcher} from '../../editor-dispatcher';
import {CheckboxFieldComponent} from '../../checkbox-field/checkbox-field-component';
import {AlertComponent} from '../../alert/alert-component';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {ElementWidthSelector} from '../../element-width-selector/element-width-selector';
import {normalizeElementWeight} from '../../../utils/element-widths';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {DataKeyDefinitionField} from '../../data-key-definition-field/data-key-definition-field';
import {isGroupLayout} from '../../../models/elements/form/layout/group-layout';
import {collectHttpMappingProblems, suggestDestinationKey} from './element-tree-editor-content-tab-properties-utils';

const summaryLayoutHiddenElementSpecificPropertyTypes = new Set<ElementType>([
    ElementType.ChipInput,
    ElementType.FileUpload,
    ElementType.MapPoint,
    ElementType.Text,
]);

export function ElementTreeEditorContentTabProperties<T extends AnyElement>() {
    const dispatch = useAppDispatch();

    const {
        root,
        editable,
        allElements,
        allowElementIdEditing,
        displayContext,
    } = useElementTreeContext();

    const {
        currentElement,
        originalElement,
        dataKeyUnlocked = false,
        onUnlockDataKey,
        onLockDataKey,
        dataKeyValidationRequested,
        onChangeCurrentElement,
        parents,
    } = useElementTreeEditorContext<T>();

    const {
        type,
    } = currentElement;

    const isRoot = root.id === currentElement.id;

    const hasSummaryLayoutParent = useMemo(() => {
        return parents.some(p => p.type === ElementType.SummaryLayout);
    }, [parents]);

    const replicatingParents = useMemo(() => {
        return parents.filter((p) => p.type === ElementType.ReplicatingContainer);
    }, [parents]);

    const replicatingParentDestinationKeyError = useMemo(() => {
        if (!replicatingParents.some((p) => isStringNullOrEmpty(p.destinationKey))) {
            return undefined;
        }

        return 'Mindestens eine übergeordnete Feldgruppe für mehrere Einträge hat noch keinen Datenschlüssel.';
    }, [replicatingParents]);

    const replicatingParentDestinationKeyPrefix = useMemo(() => {
        if (replicatingParents.length === 0 || replicatingParentDestinationKeyError !== undefined) {
            return undefined;
        }

        return replicatingParents
            .map((p) => p.destinationKey)
            .filter((key): key is string => !isStringNullOrEmpty(key))
            .join('[*].') + '[*].';
    }, [replicatingParentDestinationKeyError, replicatingParents]);

    const tabDescription = useMemo(() => {
        return getTabDescription(type);
    }, [type]);

    const httpKeyProblems = useMemo(() => {
        if (isAnyInputElement(currentElement)) {
            return collectHttpMappingProblems({element: currentElement, parents}, allElements);
        }
        return [];
    }, [currentElement, parents, allElements]);

    const normalizedWeight = useMemo(() => {
        if (isRoot) {
            return undefined;
        }

        if (!isAnyFormElement(currentElement)) {
            return undefined;
        }

        return normalizeElementWeight(currentElement.type, currentElement.weight);
    }, [currentElement, isRoot]);

    const showInternalNameField = !isRoot;
    const showElementWidthSelector = isAnyFormElement(currentElement) && !isRoot && !hasSummaryLayoutParent;
    const showInputTitleField = isAnyInputElement(currentElement);
    const showInputHintField = isAnyInputElement(currentElement) && !hasSummaryLayoutParent;
    const hasBasicProperties =
        showElementWidthSelector ||
        showInputTitleField ||
        showInputHintField;
    const hasElementSpecificProperties = hasAvailableElementSpecificProperties(currentElement, hasSummaryLayoutParent);
    const showNoConfigurableRootPropertiesHint = isRoot && !hasBasicProperties && !hasElementSpecificProperties;

    useEffect(() => {
        if (isRoot || !isAnyFormElement(currentElement)) {
            return;
        }

        if (currentElement.weight == null) {
            return;
        }

        if (normalizedWeight === currentElement.weight) {
            return;
        }

        onChangeCurrentElement({
            ...currentElement,
            weight: normalizedWeight,
        });
    }, [currentElement, isRoot, normalizedWeight, onChangeCurrentElement]);

    return (
        <>
            <ElementEditorSectionHeader
                title={tabDescription.title}
                disableMarginTop
            >
                {tabDescription.description}
            </ElementEditorSectionHeader>

            {
                tabDescription.isElement &&
                hasBasicProperties &&
                <ElementEditorSectionHeader
                    title="Grundlegende Angaben"
                    variant="h5"
                />
            }

            {
                hasBasicProperties &&
                <Grid
                    container
                    columnSpacing={4}
                >
                    {
                        showInputTitleField &&
                        <Grid
                            size={{xs: 12, lg: 6}}
                        >
                            <TextFieldComponent
                                value={currentElement.label}
                                label="Titel"
                                onChange={(val) => {
                                    onChangeCurrentElement({
                                        ...currentElement,
                                        label: val,
                                    });
                                }}
                                hint="Dieser Titel wird als Beschriftung des Feldes angezeigt. Nutzen Sie eine kurze, aussagekräftige Bezeichnung."
                                disabled={!editable}
                            />
                        </Grid>
                    }

                    {
                        isAnyInputElement(currentElement) && displayContext !== ElementDisplayContext.DataObjectSchema &&
                        <Grid size={{xs: 12, lg: 6}}>
                            <DataKeyDefinitionField
                                key={currentElement.id}
                                value={currentElement.destinationKey}
                                persistedValue={originalElement != null && isAnyInputElement(originalElement)
                                    ? originalElement.destinationKey : currentElement.destinationKey}
                                source={currentElement.label}
                                onChange={(destinationKey) => onChangeCurrentElement({...currentElement, destinationKey} as T)}
                                unlocked={dataKeyUnlocked}
                                onUnlock={() => onUnlockDataKey?.()}
                                onLock={() => onLockDataKey?.()}
                                disabled={!editable}
                                prefix={replicatingParentDestinationKeyPrefix}
                                error={replicatingParentDestinationKeyError}
                                validationRequested={dataKeyValidationRequested}
                                resolveGeneratedKey={(key) => suggestDestinationKey(key, {element: currentElement, parents}, allElements) ?? key}
                            />
                            {httpKeyProblems.length > 0 && <AlertComponent title="Warnungen zu Ihrem gewählten Datenschlüssel" color="warning">
                                <ul>{httpKeyProblems.map((problem, index) => <li key={index}>{problem}</li>)}</ul>
                            </AlertComponent>}
                        </Grid>
                    }

                    {
                        showElementWidthSelector &&
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                            }}
                        >
                            <ElementWidthSelector
                                label="Breite (Darstellung)"
                                elementType={currentElement.type}
                                value={currentElement.weight}
                                onChange={(weight) => {
                                    onChangeCurrentElement({
                                        ...currentElement,
                                        weight,
                                    });
                                }}
                                hint="Legen Sie die Breite des Elements für Tablets & Desktops fest. Auf Mobilgeräten wird die volle Breite verwendet."
                                disabled={!editable}
                            />
                        </Grid>
                    }

                    {
                        showInputHintField &&
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                            }}
                        >
                            <TextFieldComponent
                                value={currentElement.hint}
                                label="Hinweis"
                                onChange={(val) => {
                                    onChangeCurrentElement({
                                        ...currentElement,
                                        hint: val,
                                    });
                                }}
                                hint="Geben Sie bei Bedarf Hinweise für ausfüllende Personen an. Sie werden unter dem Formularelement angezeigt."
                                disabled={!editable}
                            />
                        </Grid>
                    }
                </Grid>
            }

            {
                isAnyInputElement(currentElement) &&
                !hasSummaryLayoutParent &&
                <Grid
                    container
                    columnSpacing={4}
                >
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                            xl: 3,
                        }}
                    >
                        <CheckboxFieldComponent
                            label="Pflichtangabe"
                            value={currentElement.required ?? undefined}
                            onChange={(checked) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    required: checked,
                                    disabled: false,
                                    technical: false,
                                });
                            }}
                            hint="Pflichtangaben müssen von den ausfüllenden Personen ausgefüllt werden."
                            disabled={!editable || Boolean(currentElement.disabled) || Boolean(currentElement.technical)}
                        />
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                            xl: 3,
                        }}
                    >
                        <CheckboxFieldComponent
                            label="Eingabe deaktiviert"
                            value={currentElement.disabled ?? undefined}
                            onChange={(checked) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    required: false,
                                    disabled: checked,
                                    technical: false,
                                });
                            }}
                            hint="Deaktivierte Eingaben können nicht bearbeitet werden."
                            disabled={!editable || Boolean(currentElement.required) || Boolean(currentElement.technical)}
                        />
                    </Grid>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                            xl: 3,
                        }}
                    >
                        <CheckboxFieldComponent
                            label="Verborgenes Feld"
                            value={currentElement.technical ?? undefined}
                            onChange={(checked) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    required: false,
                                    disabled: false,
                                    technical: checked,
                                });
                            }}
                            hint="Wird im Formular nicht angezeigt und kann nicht ausgefüllt werden. Der Wert des Feldes kann berechnet und weiterverwendet werden."
                            disabled={!editable || Boolean(currentElement.required) || Boolean(currentElement.disabled)}
                        />
                    </Grid>
                </Grid>
            }

            {
                tabDescription.isElement &&
                hasElementSpecificProperties &&
                <ElementEditorSectionHeader
                    title="Elementspezifische Eigenschaften"
                    variant="h5"
                />
            }

            {/* TODO: Replace this with a better EditorDispatcher */}
            {
                hasElementSpecificProperties &&
                <EditorDispatcher
                    props={currentElement}
                    onPatch={(patch) => {
                        onChangeCurrentElement({
                            ...currentElement,
                            ...patch,
                        });
                    }}
                    editable={editable}
                    scope={'application' /* TODO: remove this */}
                    hasSummaryLayoutParent={hasSummaryLayoutParent}
                    hasReplicatingContainerParent={replicatingParents.length > 0}
                />
            }

            {
                showNoConfigurableRootPropertiesHint &&
                <AlertComponent
                    title="Keine Eigenschaften verfügbar"
                    color="info"
                    sx={{mt: 4}}
                >
                    Für dieses Wurzelelement stehen keine grundlegenden oder elementspezifischen Einstellungen zur
                    Verfügung.
                </AlertComponent>
            }

            <ElementEditorSectionHeader
                title="Weitere Angaben zum Element"
                sx={{mt: 8}}
            />

            <Grid
                container
                columnSpacing={4}
            >
                {
                    showInternalNameField &&
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Bezeichnung in der Formularstruktur"
                            value={currentElement.name}
                            onChange={(val) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    name: val ?? '',
                                });
                            }}
                            hint="Eine interne Bezeichnung, z. B. eine Abkürzung, kann helfen, das Element in der Formularstruktur leichter wiederzufinden. Ohne diese Angabe wird der Titel, die Feldbeschriftung oder die Elementart verwendet. Im Formular wird die interne Bezeichnung nicht angezeigt."
                            maxCharacters={30}
                            disabled={!editable}
                        />
                    </Grid>
                }

                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="ID des Elements"
                        value={currentElement.id ?? ''}
                        onChange={(id) => {
                            onChangeCurrentElement({
                                ...currentElement,
                                id: id ?? '',
                            });
                        }}
                        hint="Technische Kennung zur eindeutigen Identifikation des Elements. Sie wird automatisch vergeben und ist vor allem für Entwickler:innen relevant."
                        disabled={!editable || (!allowElementIdEditing && displayContext != ElementDisplayContext.DataObjectSchema)}
                        endAction={{
                            icon: <ContentPasteIcon/>,
                            onClick: async () => {
                                const success = await copyToClipboardText(currentElement.id);
                                if (success) {
                                    dispatch(showSuccessSnackbar('Element-ID in Zwischenablage kopiert'));
                                } else {
                                    dispatch(showErrorSnackbar('Element-ID konnte nicht in Zwischenablage kopiert werden'));
                                }
                            },
                        }}
                    />
                </Grid>
            </Grid>
        </>
    );
}

function getTabDescription(type: ElementType) {
    switch (type) {
        case ElementType.FormLayout:
            return {
                title: 'Eigenschaften des Formulars',
                description: 'Hier konfigurieren Sie allgemeine Eigenschaften des gesamten Formulars, z. B. Titel oder technische Einstellungen.',
                isElement: false,
            };
        case ElementType.IntroductionStep:
            return {
                title: 'Eigenschaften für den Abschnitt „Allgemeine Informationen“',
                description: 'Dieser Abschnitt führt ausfüllende Personen in das Formular ein, zum Beispiel mit Informationen zu benötigten Dokumenten und möglichen Kosten.',
                isElement: false,
            };
        case ElementType.SummaryStep:
            return {
                title: 'Eigenschaften für den Abschnitt „Zusammenfassung“',
                description: 'Hier erhalten ausfüllende Personen eine Übersicht ihrer bisherigen Angaben.',
                isElement: false,
            };
        case ElementType.SubmitStep:
            return {
                title: 'Eigenschaften für den Abschnitt „Abschließende Informationen und Captcha“',
                description: 'In diesem Abschnitt prüfen und übermitteln ausfüllende Personen ihre Angaben. Hier können Sie zusätzliche Hinweise zur Übermittlung und zum weiteren Ablauf angeben.',
                isElement: false,
            };
        case ElementType.Step:
            return {
                title: 'Eigenschaften des Abschnitts',
                description: 'Konfigurieren Sie diesen Abschnitt des Formulars, z. B. mit eigenen logischen Bedingungen.',
                isElement: false,
            };
        default:
            return {
                title: 'Eigenschaften des Elements',
                description: 'Legen Sie fest, wie dieses Formularelement dargestellt wird, welche Inhalte erfasst werden können und wie sich das Element gegenüber Nutzer:innen verhält.',
                isElement: true,
            };
    }
}

function hasAvailableElementSpecificProperties(element: AnyElement, hasSummaryLayoutParent: boolean): boolean {
    if (editors[element.type] == null) {
        return false;
    }

    if (isGroupLayout(element) && element.marketplaceLink == null) {
        return false;
    }

    // elements without additional properties – should be replaced with a more generic check if element contains additional properties
    return !(
        hasSummaryLayoutParent &&
        summaryLayoutHiddenElementSpecificPropertyTypes.has(element.type)
    );
}
