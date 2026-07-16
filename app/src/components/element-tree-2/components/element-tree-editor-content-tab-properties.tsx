import {ElementType} from '../../../data/element-type/element-type';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeEditorContext} from './element-tree-editor-context';
import React, {ReactNode, useEffect, useMemo} from 'react';
import {ElementEditorSectionHeader} from '../../element-editor-section-header/element-editor-section-header';
import {Grid, Typography} from '@mui/material';
import {TextFieldComponent} from '../../text-field/text-field-component';
import ContentPasteIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useElementTreeContext} from '../element-tree-context';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {isAnyFormElement} from '../../../models/elements/form/any-form-element';
import {AnyInputElement, isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {editors} from '../../../editors';
import {EditorDispatcher} from '../../editor-dispatcher';
import {CheckboxFieldComponent} from '../../checkbox-field/checkbox-field-component';
import {AlertComponent} from '../../alert/alert-component';
import {ElementWithParents, generateElementNameWithParent} from '../../../utils/flatten-elements';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {createElementEditorNavigationLink} from '../../../hooks/use-element-editor-navigation';
import {ElementWidthSelector} from '../../element-width-selector/element-width-selector';
import {normalizeElementWeight} from '../../../utils/element-widths';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {ProcessDataKeyInputComponent} from '../../../views/process-data-key-input-field-view';
import {isGroupLayout} from '../../../models/elements/form/layout/group-layout';

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

        return 'Mindestens ein übergeordnetes Strukturiertes Listeneingabe-Element hat noch keinen Datenschlüssel.';
    }, [replicatingParents]);

    const replicatingParentDestinationKeyPrefix = useMemo(() => {
        if (replicatingParents.length === 0 || replicatingParentDestinationKeyError !== undefined) {
            return undefined;
        }

        return replicatingParents
            .map((p) => p.destinationKey)
            .filter((key): key is string => !isStringNullOrEmpty(key))
            .join('.') + '.*.';
    }, [replicatingParentDestinationKeyError, replicatingParents]);

    const tabDescription = useMemo(() => {
        return getTabDescription(type);
    }, [type]);

    const httpKeyProblems = useMemo(() => {
        if (isAnyInputElement(currentElement)) {
            return collectHttpMappingProblems(currentElement, allElements);
        }
        return [];
    }, [currentElement, allElements]);

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
        showInternalNameField ||
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
                        showInternalNameField &&
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                            }}
                        >
                            <TextFieldComponent
                                label="Interner Name"
                                value={currentElement.name}
                                onChange={(val) => {
                                    onChangeCurrentElement({
                                        ...currentElement,
                                        name: val ?? '',
                                    });
                                }}
                                hint="Vergeben Sie einen internen Namen zur besseren Identifikation. Nur für Sie und Ihr Team sichtbar."
                                maxCharacters={30}
                                disabled={!editable}
                            />
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
                                label="Breite des Elements in der Darstellung"
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
                        showInputTitleField &&
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                            }}
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
                                hint="Dieser Titel wird als Label für dieses Feld im Formular angezeigt und ist u. A. relevant für die Barrierefreiheit."
                                disabled={!editable}
                                softLimitCharacters={20}
                                softLimitCharactersWarning={'Halten Sie das Label so kurz wie möglich (empfohlen max. 20 Zeichen), da es sonst auf kleinen Bildschirmen abgeschnitten werden kann.'}
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
                                hint="Geben Sie hier zusätzliche Hinweise zur Eingabe für Antragstellende an (optional, wird unter dem Eingabefeld angezeigt)."
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
                            hint="Pflichtangaben müssen von den antragstellenden Personen ausgefüllt werden."
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
                            label="Technisches Feld"
                            value={currentElement.technical ?? undefined}
                            onChange={(checked) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    required: false,
                                    disabled: false,
                                    technical: checked,
                                });
                            }}
                            hint="Technische Felder sind für Antragstellende unsichtbar und nicht bearbeitbar."
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

            {
                isAnyInputElement(currentElement) &&
                displayContext != ElementDisplayContext.DataObjectSchema &&
                <>
                    <ElementEditorSectionHeader
                        title="Datenzuordnung im Prozess"
                        sx={{mt: 8}}
                    >
                        Legt fest, unter welchem Datenschlüssel der Wert dieses Feldes in den Vorgangsdaten gespeichert
                        und ggf. aus vorhandenen Vorgangsdaten vorbelegt wird. Ohne Datenschlüssel wird der Wert unter
                        der ID des Feldes ausschließlich in den Elementdaten gespeichert. Werte können unter Nutzung
                        einer Punktnotation in verschachtelte Datenstrukturen geschrieben und gelesen werden (z. B.
                        „person.vorname“).
                    </ElementEditorSectionHeader>

                    <Grid
                        container
                        columnSpacing={4}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                lg: 6,
                                xl: 6,
                            }}
                        >
                            <ProcessDataKeyInputComponent
                                label="Datenschlüssel"
                                value={currentElement.destinationKey}
                                onChange={(val) => {
                                    onChangeCurrentElement({
                                        ...currentElement,
                                        destinationKey: val,
                                    } as T);
                                }}
                                disabled={!editable}
                                disableWildCards={true}
                                prefix={replicatingParentDestinationKeyPrefix}
                                error={replicatingParentDestinationKeyError}
                            />
                        </Grid>
                    </Grid>

                    {
                        httpKeyProblems.length > 0 &&
                        <AlertComponent
                            title="Warnungen zu Ihrem gewählten Datenschlüssel"
                            color="warning"
                        >
                            <ul>
                                {
                                    httpKeyProblems
                                        .map((problem, index) => (
                                            <li key={index}>
                                                <Typography>
                                                    {problem}
                                                </Typography>
                                            </li>
                                        ))
                                }
                            </ul>
                        </AlertComponent>
                    }

                </>
            }

            <ElementEditorSectionHeader
                title="Technische Informationen für Entwickler:innen"
                sx={{mt: 8}}
            >
                Hier finden Sie technische Zusatzinformationen, die insbesondere für Entwickler:innen von Bedeutung sein
                können.
            </ElementEditorSectionHeader>

            <Grid
                container
                columnSpacing={4}
            >
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
                        disabled={!allowElementIdEditing && displayContext != ElementDisplayContext.DataObjectSchema}
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
                description: 'Dieser Abschnitt dient der Einführung des Antragstellers in das Formular – z. B. mit einleitenden Informationen und einem Überblick über wichtige Eckdaten wie mögliche Gebühren.',
                isElement: false,
            };
        case ElementType.SummaryStep:
            return {
                title: 'Eigenschaften für den Abschnitt „Zusammenfassung“',
                description: 'Hier wird dem Antragstellenden eine Übersicht der bisherigen Eingaben angezeigt.',
                isElement: false,
            };
        case ElementType.SubmitStep:
            return {
                title: 'Eigenschaften für den Abschnitt „Absenden des Antrages“',
                description: 'In diesem Schritt wird der Antrag final geprüft und abgesendet. Darüber hinaus können hier weitere Informationen angegeben werden, z. B. zur Bearbeitung des Antrages.',
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

    if (isGroupLayout(element) && element.storeLink == null) {
        return false;
    }

    // elements without additional properties – should be replaced with a more generic check if element contains additional properties
    return !(
        hasSummaryLayoutParent &&
        summaryLayoutHiddenElementSpecificPropertyTypes.has(element.type)
    );
}

function collectHttpMappingProblems(element: AnyInputElement, allElements: ElementWithParents[]): ReactNode[] {
    if (element.destinationKey == null || isStringNullOrEmpty(element.destinationKey)) {
        return [];
    }

    const problems: ReactNode[] = [];

    for (const ot of allElements) {
        const {
            element: otherElement,
        } = ot;

        if (element.id === otherElement.id) {
            continue;
        }

        if (!isAnyInputElement(otherElement)) {
            continue;
        }

        if (otherElement.destinationKey == null || isStringNullOrEmpty(otherElement.destinationKey)) {
            continue;
        }

        if (otherElement.destinationKey === element.destinationKey && element.destinationKey != null) {
            const otherElementName = generateElementNameWithParent(ot);

            problems.push(
                <>
                    <Typography>
                        Der Datenschlüssel <strong>„{element.destinationKey}”</strong> wird bereits von
                        dem Formularelement <a href={createElementEditorNavigationLink(otherElement.id, DefaultTabs.properties)}>„{otherElementName}”</a> verwendet.
                        Dies führt dazu, dass die Daten gegebenenfalls überschrieben werden. Stellen Sie sicher, dass
                        dies ein beabsichtigtes Verhalten ist.
                    </Typography>
                </>,
            );
        }

        if (otherElement.destinationKey.startsWith(element.destinationKey + '.') || element.destinationKey.startsWith(otherElement.destinationKey + '.')) {
            const otherElementName = generateElementNameWithParent(ot);

            const otherElementWritesParent = otherElement
                .destinationKey
                .startsWith(element.destinationKey + '.');

            problems.push(
                <>
                    <Typography gutterBottom>
                        Der Datenschlüssel <strong>„{element.destinationKey}”</strong> überschneidet sich
                        mit dem Datenschlüssel <strong>„{otherElement.destinationKey}”</strong> des
                        Formularelements <a href={createElementEditorNavigationLink(otherElement.id, DefaultTabs.metadata)}>„{otherElementName}”</a>.
                        {
                            otherElementWritesParent ?
                                ' Das andere Element schreibt in ein Unterattribut des aktuellen Elements.' :
                                ' Das aktuelle Element schreibt in ein Unterattribut des anderen Elements.'
                        }
                    </Typography>
                    <Typography>
                        Dies kann zu Problemen bei der Datenverarbeitung führen. Bitte passen Sie die Schlüssel an, um
                        Überschneidungen zu vermeiden oder stellen Sie sicher, dass nicht beide Elemente gleichzeitig
                        verwendet werden.
                    </Typography>
                </>,
            );
        }
    }

    return problems;
}
