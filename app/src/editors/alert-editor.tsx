import React from 'react';
import {type AlertColor, Grid} from '@mui/material';
import {type AlertElement} from '../models/elements/form/content/alert-element';
import {type BaseEditor} from './base-editor';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {RichTextInputComponent} from '../components/rich-text-input-component/rich-text-input-component';

const colors = [
    ['success', 'Erfolg'],
    ['info', 'Information'],
    ['warning', 'Warnung'],
    ['error', 'Fehler'],
];

export const AlertEditor: BaseEditor<AlertElement, ElementTreeEntity> = ({
                                                                             element,
                                                                             onPatch,
                                                                             editable,
                                                                         }) => {
    return (
        <>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <TextFieldComponent
                        value={element.title}
                        label="Titel"
                        onChange={(val) => {
                            onPatch({
                                title: val,
                            });
                        }}
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
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
                        disabled={!editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <RichTextInputComponent
                        value={element.text ?? ''}
                        label="Text"
                        onChange={(value) => {
                            onPatch({text: value ?? undefined});
                        }}
                        disabled={!editable}
                    />
                </Grid>
            </Grid>
        </>
    );
};
