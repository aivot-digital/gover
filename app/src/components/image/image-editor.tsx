import React, { type JSX } from 'react';
import {type ImageElement} from '../../models/elements/form/content/image-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from "../../utils/string-utils";
import {Alert, AlertTitle, Grid, Typography} from '@mui/material';
import AccessibilityNewIcon from '@mui/icons-material/AccessibilityNew';

export function ImageEditor(props: BaseEditorProps<ImageElement, ElementTreeEntity>): JSX.Element {
    return (
        <>
            <TextFieldComponent
                value={props.element.src ?? ''}
                label="URL (Link zur Grafikdatei)"
                onChange={(val) => {
                    props.onPatch({
                        src: val,
                    });
                }}
                disabled={!props.editable}
            />
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
                        value={props.element.caption ?? ''}
                        label="Bildbeschreibung (Caption)"
                        onChange={(val) => {
                            props.onPatch({
                                caption: val,
                            });
                        }}
                        hint="Die Bildbeschreibung erscheint unter dem Bild und ergänzt Informationen, z. B. Erläuterungen oder Urheberangaben."
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <TextFieldComponent
                        value={props.element.alt ?? ''}
                        label="Alternativtext (Alt-Text)"
                        onChange={(val) => {
                            props.onPatch({
                                alt: val,
                            });
                        }}
                        hint="Der Alternativtext beschreibt den Bildinhalt für Nutzer mit Sehbehinderungen und sorgt so für Barrierefreiheit. Bitte kurz und aussagekräftig formulieren."
                        error={isStringNullOrEmpty(props.element.alt) && isStringNotNullOrEmpty(props.element.src) ? 'Im Sinne der Barrierefreiheit sollten Sie immer einen Alternativtext für das Bild angeben.' : undefined}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
            <Alert severity="info" sx={{mt: 4}} icon={<AccessibilityNewIcon/>}>
                <AlertTitle>Hinweis zur Barrierefreiheit von eingebundenen Bildern</AlertTitle>
                <Typography sx={{maxWidth: 860}}>
                    Bitte beachten Sie, dass eingebundene Bilder ein Kontrastverhältnis von mindestens 3:1 aufweisen
                    müssen, um den Anforderungen der Barrierefreiheit gemäß
                    der <abbr title={'Web Content Accessibility Guidelines'}>WCAG</abbr> 2.1 (AA) zu entsprechen. Dies
                    gilt für die Unterscheidung von grafischen Objekten in den Bildern (wie z.B. Symbole und Teile von
                    Diagrammen oder anderen Grafiken).
                </Typography>
            </Alert>
        </>
    );
}
