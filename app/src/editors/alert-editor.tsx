import { type AlertColor } from '@mui/material';
import { type AlertElement } from '../models/elements/form/content/alert-element';
import { type BaseEditor } from './base-editor';
import { TextFieldComponent } from '../components/text-field/text-field-component';
import { SelectFieldComponent } from '../components/select-field/select-field-component';

const colors = [
    ['success', 'Erfolg'],
    ['info', 'Information'],
    ['warning', 'Warnung'],
    ['error', 'Fehler'],
];

export const AlertEditor: BaseEditor<AlertElement> = ({ element, onPatch }) => {
    return (
        <>
            <TextFieldComponent
                value={element.title}
                label="Titel"
                onChange={(val) => {
                    onPatch({
                        title: val,
                    });
                }}
            />

            <TextFieldComponent
                value={element.text}
                label="Hinweis"
                multiline
                onChange={(val) => {
                    onPatch({
                        text: val,
                    });
                }}
            />

            <SelectFieldComponent
                label="Hinweistyp"
                value={element.alertType ?? 'info'}
                onChange={(val) => {
                    onPatch({
                        alertType: val as AlertColor,
                    });
                }}
                options={colors.map(([type, label]) => ({
                    label,
                    value: type,
                }))}
            />
        </>
    );
};
