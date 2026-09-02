import React, {useState} from 'react';
import {type ImageElement} from '../../models/elements/form/content/image-element';
import {type BaseEditorProps} from '../../editors/base-editor';
import {TextFieldComponent} from '../text-field/text-field-component';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {Alert, AlertTitle, Grid, Typography} from '@mui/material';
import AccessibilityNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/AccessibilityNew';
import ImageSearchOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ImageSearch';
import {SelectAssetDialog} from '../../dialogs/select-asset-dialog/select-asset-dialog';
import {AssetsApiService} from '../../modules/assets/assets-api-service';

export function ImageEditor(props: BaseEditorProps<ImageElement>) {
    const [showImageSearch, setShowImageSearch] = useState(false);

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
                endAction={{
                    icon: <ImageSearchOutlinedIcon />,
                    tooltip: 'Bild suchen',
                    onClick: () => {
                        setShowImageSearch(true);
                    },
                }}
            />
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
                        lg: 6,
                    }}
                >
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
            <Alert
                severity="info"
                sx={{mt: 4}}
                icon={<AccessibilityNewIcon />}
            >
                <AlertTitle>Hinweise zu Darstellung und Barrierefreiheit</AlertTitle>
                <Typography
                    component="h3"
                    variant="subtitle2"
                    sx={{mt: 1.5, mb: 0.5}}
                >
                    Darstellung auf hellen und dunklen Hintergründen
                </Typography>
                <Typography sx={{maxWidth: 860}}>
                    Bilder werden in der hellen und dunklen Darstellung unverändert verwendet. Verwenden Sie
                    insbesondere für Grafiken und Diagramme möglichst eine Bilddatei mit eigenem, deckendem
                    Hintergrund. Stellen Sie bei transparenten Bildern sicher, dass alle wichtigen Inhalte auf hellen
                    wie dunklen Flächen gut erkennbar bleiben.
                </Typography>

                <Typography
                    component="h3"
                    variant="subtitle2"
                    sx={{mt: 2, mb: 0.5}}
                >
                    Barrierefreiheit
                </Typography>
                <Typography sx={{maxWidth: 860}}>
                    Grafische Elemente, die für das Verständnis erforderlich sind, müssen nach den
                    {' '}<abbr title="Web Content Accessibility Guidelines">WCAG</abbr> 2.2 in der Regel mit
                    mindestens 3:1 zu angrenzenden Farben kontrastieren. Verwenden Sie für wichtige Informationen
                    möglichst echten Text statt Schrift in Bildern und hinterlegen Sie einen aussagekräftigen
                    Alternativtext.
                </Typography>
            </Alert>

            <SelectAssetDialog
                title="Bild auswählen"
                show={showImageSearch}
                mimetype="image/"
                onSelect={(assetKey) => {
                    props.onPatch({
                        src: AssetsApiService.useAssetLink(assetKey),
                    });
                    setShowImageSearch(false);
                }}
                onCancel={() => {
                    setShowImageSearch(false);
                }}
                mode="public"
            />
        </>
    );
}
