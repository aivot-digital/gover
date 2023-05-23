import {DefaultTabs} from '../../data/default-tabs';
import {
    Box,
    Checkbox,
    FormControl,
    FormControlLabel,
    FormHelperText,
    IconButton,
    InputLabel,
    MenuItem,
    Select,
    TextField,
    Tooltip
} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faArrowsRotate} from '@fortawesome/pro-light-svg-icons';
import {EditorDispatcher} from '../../../../../editor-dispatcher';
import {CodeTab} from '../../tabs/code-tab/code-tab';
import React from 'react';
import {StructureTab} from '../../tabs/structure-tab/structure-tab';
import {checkId, generateElementIdForType} from '../../../../../../utils/id-utils';
import {TestTab} from '../../tabs/test-tab/test-tab';
import {ElementType} from '../../../../../../data/element-type/element-type';
import {selectLoadedApplication} from '../../../../../../slices/app-slice';
import {useAppSelector} from '../../../../../../hooks/use-app-selector';
import {AnyElement} from '../../../../../../models/elements/any-element';
import {ElementEditorContentProps} from './element-editor-content-props';
import {AnyFormElement} from '../../../../../../models/elements/form/any-form-element';
import {BaseInputElement} from "../../../../../../models/elements/form/base-input-element";
import {isAnyInputElement} from "../../../../../../models/elements/form/input/any-input-element";

export function ElementEditorContent<T extends AnyElement>({
                                                               element,
                                                               currentTab,
                                                               additionalTabs,
                                                               onChange,
                                                           }: ElementEditorContentProps<T>) {
    const application = useAppSelector(selectLoadedApplication);

    const idError = application != null ? checkId(application.root, element.id) : null;

    const handleUpdate = (patch: Partial<AnyElement>) => {
        onChange({
            ...element,
            ...patch,
        })
    };

    switch (currentTab) {
        case DefaultTabs.properties:
            return (
                <Box sx={{p: 4}}>
                    <Box sx={{display: 'flex', alignItems: 'center'}}>
                        <TextField
                            label="ID (für Entwickler:innen)"
                            value={element.id ?? ''}
                            fullWidth
                            margin="normal"
                            onChange={event => {
                                handleUpdate({id: event.target.value ?? ''});
                            }}
                            helperText={idError != null ? idError : 'Verwenden Sie bitte ausschließlich alphanumerische Zeichen und Unterstriche.'}
                            error={idError != null}
                        />
                        <Box sx={{ml: 2, transform: 'translateY(-6px)'}}>
                            <Tooltip title="ID neu generieren">
                                <IconButton
                                    onClick={() => {
                                        const res = window.confirm('Sind Sie sicher, dass Sie eine neue, zufällige ID generieren möchten?');
                                        if (res) {
                                            handleUpdate({id: generateElementIdForType(element.type)});
                                        }
                                    }}
                                >
                                    <FontAwesomeIcon icon={faArrowsRotate}/>
                                </IconButton>
                            </Tooltip>
                        </Box>
                    </Box>

                    {
                        element.type !== ElementType.Root &&
                        element.type !== ElementType.Step &&
                        element.type !== ElementType.IntroductionStep &&
                        element.type !== ElementType.SummaryStep &&
                        element.type !== ElementType.SubmitStep &&
                        <>
                            <TextField
                                label="Interner Name"
                                value={element.name ?? ''}
                                fullWidth
                                margin="normal"
                                onChange={event => {
                                    handleUpdate({name: event.target.value ?? ''});
                                }}
                                helperText="Vergeben Sie einen Namen für dieses Element um es besser identifizieren zu können. Diesen Namen können nur Sie und ihre Mitarbeiter einsehen"
                            />

                            <FormControl fullWidth>
                                <InputLabel id="width-label">
                                    Breite
                                </InputLabel>
                                <Select
                                    labelId="width-label"
                                    value={(element as AnyFormElement).weight ?? 12}
                                    label="Breite"
                                    onChange={event => {
                                        handleUpdate({weight: event.target.value ?? 12} as Partial<AnyElement>);
                                    }}
                                >
                                    <MenuItem value={3}>25%</MenuItem>
                                    <MenuItem value={4}>33%</MenuItem>
                                    <MenuItem value={6}>50%</MenuItem>
                                    <MenuItem value={8}>66%</MenuItem>
                                    <MenuItem value={9}>75%</MenuItem>
                                    <MenuItem value={12}>100%</MenuItem>
                                </Select>
                                <FormHelperText>
                                    Bestimmen Sie die Breite des Anzeigeelements.
                                </FormHelperText>
                            </FormControl>
                        </>
                    }

                    <Box sx={{m: 4}}/>

                    {
                        isAnyInputElement(element) &&
                        <>
                            <TextField
                                value={element.label ?? ''}
                                label="Titel"
                                margin="normal"
                                onChange={event => handleUpdate({
                                    label: event.target.value,
                                })}
                                helperText="Dieser Text wird den Bürger:innen im Antrag angezeigt."
                            />

                            <TextField
                                value={element.hint ?? ''}
                                label="Hinweis"
                                margin="normal"
                                onChange={event => handleUpdate({
                                    hint: event.target.value,
                                })}
                                helperText="Der Hinweis sollte genutzt werden, um den Bürger:innen weitere Informationen über die Eingabe zu geben."
                            />

                            <FormControl>
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            checked={element.required ?? false}
                                            onChange={event => handleUpdate({
                                                required: event.target.checked,
                                                disabled: undefined,
                                            })}
                                            disabled={element.disabled}
                                        />
                                    }
                                    label="Pflichtangabe"
                                />

                                {
                                    element.disabled &&
                                    <FormHelperText>
                                        Deaktivierte Eingaben können keine Pflichtangaben sein.
                                    </FormHelperText>
                                }
                            </FormControl>

                            <FormControl sx={{mb: 4}}>
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            checked={element.disabled ?? false}
                                            onChange={event => handleUpdate({
                                                required: undefined,
                                                disabled: event.target.checked,
                                            })}
                                            disabled={element.required}
                                        />
                                    }
                                    label="Eingabe deaktiviert"
                                />
                                {
                                    element.required &&
                                    <FormHelperText>
                                        Pflichtangaben können nicht deaktiviert werden.
                                    </FormHelperText>
                                }
                            </FormControl>
                        </>
                    }

                    <EditorDispatcher
                        props={element}
                        onPatch={handleUpdate}
                    />
                </Box>
            );
        case DefaultTabs.visibility:
            return (
                <CodeTab
                    key="visibility"
                    resultTitle="Sichtbarkeit festlegen"
                    resultHint="Dieses Element ist Sichtbar, wenn die folgende Funktion wahr ist."
                    element={element}
                    func={element.isVisible}
                    allowNoCode={true}
                    shouldReturnString={false}
                    onChange={updatedFunc => handleUpdate({isVisible: updatedFunc})}
                />
            );
        case DefaultTabs.validation:
            return (
                <CodeTab
                    key="validate"
                    resultTitle="Validierung durchführen"
                    resultHint="Dieses Element ist valide, wenn die folgende Funktion keine Nachricht mit einem Validierungsproblem erzeugt."
                    element={element}
                    func={(element as BaseInputElement<any, any>).validate}
                    allowNoCode={true}
                    shouldReturnString={true}
                    onChange={updatedFunc => handleUpdate({validate: updatedFunc})}
                />
            );
        case DefaultTabs.value:
            return (
                <CodeTab
                    key="value"
                    resultTitle="Dynamischen Wert bestimmen"
                    resultHint="Dieses Element bekommt den Rückgabewert der folgenden Funktion."
                    element={element}
                    func={(element as BaseInputElement<any, any>).computeValue}
                    allowNoCode={false}
                    shouldReturnString={false}
                    onChange={updatedFunc => handleUpdate({computeValue: updatedFunc})}
                />
            );
        case DefaultTabs.patch:
            return (
                <CodeTab
                    key="patch"
                    resultTitle="Element aktualisieren"
                    resultHint="Dieses Element wird mit dem Rückgabewert der folgenden Funktion aktualisiert."
                    element={element}
                    func={element.patchElement}
                    allowNoCode={false}
                    shouldReturnString={false}
                    onChange={updatedFunc => handleUpdate({patchElement: updatedFunc})}
                />
            );
        case DefaultTabs.structure:
            return (
                <StructureTab
                    elementModel={element}
                    onChange={handleUpdate}
                />
            );
        case DefaultTabs.test:
            return (
                <TestTab
                    elementModel={element}
                    onPatch={updatedTestProtocolSet => handleUpdate({
                        testProtocolSet: updatedTestProtocolSet,
                    })}
                />
            );
        default:
            if (additionalTabs.some(add => currentTab === add.label)) {
                return (
                    <Box sx={{p: 4}}>
                        <EditorDispatcher
                            props={element}
                            onPatch={handleUpdate}
                            additionalTabIndex={additionalTabs.findIndex(add => currentTab === add.label)}
                        />
                    </Box>
                );
            }
            return null;
    }
}
