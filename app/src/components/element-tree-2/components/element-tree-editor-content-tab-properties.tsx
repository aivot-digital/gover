import {ElementType} from '../../../data/element-type/element-type';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeEditorContext} from './element-tree-editor-context';
import React, {ReactNode, useMemo} from 'react';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {ElementEditorSectionHeader} from '../../element-editor-section-header/element-editor-section-header';
import {Grid, Typography} from '@mui/material';
import {TextFieldComponent} from '../../text-field/text-field-component';
import ContentPasteIcon from '@mui/icons-material/ContentPaste';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useElementTreeContext} from '../element-tree-context';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {SelectFieldComponent} from '../../select-field/select-field-component';
import {isAnyFormElement} from '../../../models/elements/form/any-form-element';
import {AnyInputElement, isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {editors} from '../../../editors';
import {EditorDispatcher} from '../../editor-dispatcher';
import {CheckboxFieldComponent} from '../../checkbox-field/checkbox-field-component';
import {LoadedForm} from '../../../slices/app-slice';
import {AlertComponent} from '../../alert/alert-component';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {isStringNullOrEmpty} from '../../../utils/string-utils';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {Link} from 'react-router-dom';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {createElementEditorNavigationLink} from '../../../hooks/use-element-editor-navigation';

export function ElementTreeEditorContentTabProperties<T extends AnyElement>() {
    const dispatch = useAppDispatch();

    const {
        editable,
        allElements,
    } = useElementTreeContext();

    const {
        currentElement,
        onChangeCurrentElement,
    } = useElementTreeEditorContext<T>();

    const {
        type,
    } = currentElement;

    const tabDescription = useMemo(() => {
        return getTabDescription(type);
    }, [type]);

    const httpKeyProblems = useMemo(() => {
        if (isAnyInputElement(currentElement)) {
            return collectHttpMappingProblems(currentElement, allElements);
        }
        return [];
    }, [currentElement, allElements]);

    return (
        <>
            <ElementEditorSectionHeader
                title={tabDescription.title}
                titleSuffix={tabDescription.isElement ? '(' + getElementNameForType(type) + ')' : undefined}
                disableMarginTop
            >
                {tabDescription.description}
            </ElementEditorSectionHeader>

            {
                tabDescription.isElement &&
                <ElementEditorSectionHeader
                    title="Grundlegende Angaben"
                    variant="h5"
                />
            }

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

                {
                    isAnyFormElement(currentElement) &&
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <SelectFieldComponent
                            label="Breite des Elements in der Darstellung"
                            value={currentElement?.weight?.toString() ?? '12'}
                            onChange={(val) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    weight: val != null ? parseFloat(val) : 12,
                                });
                            }}
                            options={WeightOptions}
                            hint="Legen Sie die Breite des Elements für Tablets & Desktops fest. Auf Mobilgeräten wird stets die volle Breite verwendet."
                            disabled={!editable}
                        />
                    </Grid>
                }

                {
                    isAnyInputElement(currentElement) &&
                    <>
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
                    </>
                }
            </Grid>

            {
                tabDescription.isElement &&
                // elements without additional properties – should be replaced with a more generic check if element contains additional properties
                editors[type] != null &&
                <ElementEditorSectionHeader
                    title="Elementspezifische Eigenschaften"
                    variant="h5"
                />
            }

            {/* TODO: Replace this with a better EditorDispatcher */}
            <EditorDispatcher
                props={currentElement}
                onPatch={(patch) => {
                    onChangeCurrentElement({
                        ...currentElement,
                        ...patch,
                    });
                }}
                entity={{} as LoadedForm}
                onPatchEntity={() => {
                }}
                editable={editable}
                scope={'application' /* TODO: remove this */}
            />

            {
                isAnyInputElement(currentElement) &&
                <>
                    <ElementEditorSectionHeader
                        title="Eingabeoptionen"
                        variant="h5"
                        disableMarginBottom
                    />

                    <Grid
                        container
                        columnSpacing={4}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                lg: 4,
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
                                lg: 4,
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
                                lg: 4,
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
                </>
            }

            {
                isAnyInputElement(currentElement) &&
                <>
                    <ElementEditorSectionHeader
                        title="Datenzuordnung"
                        sx={{mt: 8}}
                    >
                        Definieren Sie, wie dieses Formularelement mit Daten verknüpft ist.
                        Legen Sie fest, welcher Datenschlüssel zur Schnittstelle übertragen wird und welchen Attributen
                        aus Nutzerkonten (wenn aktiviert) das Element zugeordnet ist.
                    </ElementEditorSectionHeader>

                    <ElementEditorSectionHeader
                        title="Schnittstellen"
                        variant="h5"
                    />

                    <TextFieldComponent
                        value={currentElement.destinationKey ?? undefined}
                        label="HTTP-Schnittstellen-Schlüssel"
                        onChange={(val) => {
                            onChangeCurrentElement({
                                ...currentElement,
                                destinationKey: val,
                            } as T);
                        }}
                        hint="Dieser Schlüssel wird statt der Feld-ID verwendet, wenn die Daten an eine HTTP-Schnittstelle gesendet werden."
                        disabled={!editable}
                    />

                    {
                        httpKeyProblems.length > 0 &&
                        <AlertComponent
                            title="Warnungen zu Ihrem gewählten HTTP-Schnittstellenschlüssel"
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


const WeightOptions = [
    {
        label: '25%',
        value: '3',
    },
    {
        label: '33%',
        value: '4',
    },
    {
        label: '37,5%',
        value: '4.5',
    },
    {
        label: '50%',
        value: '6',
    },
    {
        label: '62,5%',
        value: '7.5',
    },
    {
        label: '66%',
        value: '8',
    },
    {
        label: '75%',
        value: '9',
    },
    {
        label: '100%',
        value: '12',
    },
];


function collectHttpMappingProblems(element: AnyInputElement, allElements: ElementWithParents[]): ReactNode[] {
    if (element.destinationKey == null || isStringNullOrEmpty(element.destinationKey)) {
        return [];
    }

    const problems: ReactNode[] = [];

    for (const ot of allElements) {
        const {
            element: otherElement,
            parents: otherElementParents,
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
            const otherElementLabel = generateComponentTitle(otherElement);
            const otherElementPath = otherElementParents
                .map(e => generateComponentTitle(e))
                .join(' > ');

            problems.push(
                <>
                    <Typography>
                        Der HTTP-Schnittstellen-Schlüssel <strong>„{element.destinationKey}”</strong> wird bereits von
                        dem
                        Formularelement <Link to={createElementEditorNavigationLink(otherElement.id, DefaultTabs.properties)}>„{otherElementPath} &gt; {otherElementLabel}”</Link> verwendet.
                        Dies führt dazu, dass die Daten gegebenenfalls überschrieben werden. Stellen Sie sicher, dass
                        dies ein beabsichtigtes Verhalten ist.
                    </Typography>
                </>,
            );
        }

        if (otherElement.destinationKey.startsWith(element.destinationKey + '.') || element.destinationKey.startsWith(otherElement.destinationKey + '.')) {
            const otherElementLabel = generateComponentTitle(otherElement);
            const otherElementPath = otherElementParents.map(e => generateComponentTitle(e)).join(' > ');

            const otherElementWritesParent = otherElement
                .destinationKey
                .startsWith(element.destinationKey + '.');

            problems.push(
                <>
                    <Typography gutterBottom>
                        Der HTTP-Schnittstellen-Schlüssel <strong>„{element.destinationKey}”</strong> überschneidet sich
                        mit dem HTTP-Schnittstellen-Schlüssel <strong>„{otherElement.destinationKey}”</strong> des
                        Formularelements <Link to={createElementEditorNavigationLink(otherElement.id, DefaultTabs.metadata)}>„{otherElementPath} &gt; {otherElementLabel}”</Link>.
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