import React from 'react';
import {type ImageElement} from '../../models/elements/form/content/image-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from "../../utils/string-utils";
import {Alert, AlertTitle, Typography} from "@mui/material";
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

            <TextFieldComponent
                value={props.element.caption ?? ''}
                label="Bildbeschreibung (Caption)"
                onChange={(val) => {
                    props.onPatch({
                        caption: val,
                    });
                }}
                hint="Die Bildbeschreibung wird unter dem Bild angezeigt und liefert zusätzliche Informationen für Nutzer. Sie kann den Inhalt des Bildes erklären oder relevante Details ergänzen (z. B. Urheberrechtsangaben)."
                disabled={!props.editable}
            />

            <TextFieldComponent
                value={props.element.alt ?? ''}
                label="Alternativtext (Alt-Text)"
                onChange={(val) => {
                    props.onPatch({
                        alt: val,
                    });
                }}
                hint="Der Alternativtext wird verwendet, wenn das Bild nicht geladen werden kann oder wird für Nutzer mit Sehbehinderungenvon durch Screenreader vorgelesen. Geben Sie eine kurze, prägnante Beschreibung des Bildinhalts ein, die alle wichtigen Informationen vermittelt."
                error={isStringNullOrEmpty(props.element.alt) && isStringNotNullOrEmpty(props.element.src) ? 'Im Sinne der Barrierefreiheit sollten Sie immer einen Alternativtext für das Bild angeben.' : undefined}
                disabled={!props.editable}
            />

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
