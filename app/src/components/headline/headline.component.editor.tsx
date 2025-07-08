import {type HeadlineElement} from '../../models/elements/form/content/headline-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {Grid} from '@mui/material';

import type { JSX } from "react";

export function HeadlineComponentEditor(props: BaseEditorProps<HeadlineElement, ElementTreeEntity>): JSX.Element {
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
                        value={props.element.content ?? ''}
                        label="Überschrift"
                        onChange={(val) => {
                            props.onPatch({
                                content: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }} />
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <CheckboxFieldComponent
                        label="Kompakte Überschrift verwenden"
                        value={props.element.small}
                        onChange={(val) => {
                            props.onPatch({
                                small: val,
                            });
                        }}
                        disabled={!props.editable}
                        hint={"Diese Option zeigt die Überschrift mit kleinerer Schriftgröße und weniger Abstand an."}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <CheckboxFieldComponent
                        label="In Großbuchstaben anzeigen"
                        value={props.element.uppercase}
                        onChange={(val) => {
                            props.onPatch({
                                uppercase: val,
                            });
                        }}
                        disabled={!props.editable}
                        hint={"Diese Option zeigt die Überschrift in GROSSBUCHSTABEN an. Verwenden Sie diese Option sparsam."}
                    />
                </Grid>
            </Grid>
        </>
    );
}
