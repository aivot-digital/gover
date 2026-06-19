import {ReactEventHandler, useCallback, useEffect, useState} from 'react';
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    Grid,
    Stack,
    Typography,
    useTheme,
} from '@mui/material';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {renderToStaticMarkup} from 'react-dom/server';
import {ImageSelector} from '../../modules/assets/components/image-selector';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
import {HtmlTemplateSlot, parseHtmlTemplateSlots} from './html-template-input-utils';
import {TextFieldComponent} from '../text-field/text-field-component';
import {RichTextInputComponent} from '../rich-text-input-component/rich-text-input-component';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {VStorageIndexItemWithAssetEntity} from '../../modules/storage/entities/storage-index-item-entity';
import {DialogProps} from '@mui/material/Dialog';
import {HtmlTemplateInputValue} from '../../models/elements/form/input/html-template-input-element';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

interface HtmlTemplateInputComponentDialogProps {
    label: string;
    asset: VStorageIndexItemWithAssetEntity | null;
    slots: HtmlTemplateInputValue['slots'];
    onChangeSlots: (slots: HtmlTemplateInputValue['slots']) => void;
    onClose: () => void;
}

function renderRichTextMarkdown(markdown: string): string {
    return renderToStaticMarkup(
        <ReactMarkdown
            remarkPlugins={[
                remarkGfm,
            ]}
        >
            {markdown}
        </ReactMarkdown>,
    );
}

interface SlotToEdit {
    key: string;
    type: 'text' | 'richtext' | 'image';
    label: string;
    hint: string;
    defaultValue: string;
}

export function HtmlTemplateInputComponentDialog(props: DialogProps & HtmlTemplateInputComponentDialogProps) {
    const theme = useTheme();

    const {
        label,
        asset,
        slots,
        onChangeSlots,
        onClose,
        ...rest
    } = props;

    const [originalHeader, setOriginalHeader] = useState<string | null>(null);
    const [originalContent, setOriginalContent] = useState<string | null>(null);
    const [originalFooter, setOriginalFooter] = useState<string | null>(null);

    const [showSlotToEdit, setShowSlotToEdit] = useState(false);
    const [slotToEdit, setSlotToEdit] = useState<SlotToEdit | null>(null);
    const [editedSlotValue, setEditedSlotValue] = useState<string | null>(null);

    useEffect(() => {
        // Reset the edited slot value if the slot to edit changes to prevent old value display.
        setEditedSlotValue(null);
    }, [slotToEdit]);

    useEffect(() => {
        if (asset == null) {
            return;
        }

        new AssetsApiService()
            .downloadContentInStorageProvider(
                asset.pathFromRoot,
                asset.storageProviderId,
                false,
            )
            .then((fileContent) => {
                return fileContent.text();
            })
            .then((html) => {
                const p1 = html
                    .split('<!-- KOPFZEILE -->');

                let inhalt: string;

                let kopfzeile: string | null = null;
                if (p1.length == 1) {
                    inhalt = html;
                } else {
                    kopfzeile = p1[0];
                    inhalt = p1[1];
                }

                const p2 = inhalt
                    .split('<!-- FUSSZEILE -->');

                let fusszeile: string | null = null;
                if (p2.length == 1) {
                    inhalt = html;
                } else {
                    inhalt = p2[0];
                    fusszeile = p2[1];
                }

                setOriginalHeader(kopfzeile);
                setOriginalContent(inhalt);
                setOriginalFooter(fusszeile);
            });
        // TODO: Handle errors
    }, [asset]);

    const [slotRefs, setSlotRefs] = useState<Record<string, HTMLElement>>({});

    const handleIframeLoad: ReactEventHandler<HTMLIFrameElement> = useCallback((event) => {
        const iframe = event.target as HTMLIFrameElement;

        if (iframe == null || iframe.contentWindow === null) {
            return;
        }

        const height: number = iframe
            .contentWindow
            .document
            .body
            .scrollHeight ?? 0;
        iframe.style.height = height + 'px';

        const _slotRefs: Record<string, HTMLElement> = {};

        iframe
            .contentWindow
            .document
            .querySelectorAll('[data-slot]')
            .forEach((slotElement) => {
                const elem = slotElement as HTMLElement;
                const slotKey = elem.dataset.slot!;
                _slotRefs[slotKey] = elem;

                if (slots[slotKey] != null) {
                    elem.textContent = slots[slotKey];
                }

                elem.style.backgroundColor = theme.palette.grey[200];
                elem.style.cursor = 'pointer';
                elem.onclick = () => {
                    const type = elem.dataset.slotType as 'text' | 'richtext' | 'image' ?? 'text';
                    let defaultValue = '';
                    if (type === 'text' || type === 'richtext') {
                        defaultValue = elem.textContent?.trim() ?? '';
                    } else if (type === 'image') {
                        defaultValue = elem.getAttribute('src')?.trim() ?? '';
                    }

                    setSlotToEdit({
                        key: slotKey,
                        type: type,
                        label: elem.dataset.slotLabel ?? '',
                        hint: elem.dataset.slotHint ?? '',
                        defaultValue: defaultValue,
                    });
                    setShowSlotToEdit(true);
                };
            });

        setSlotRefs((prev) => ({
            ...prev,
            ..._slotRefs,
        }));
    }, []);

    const handleClose = () => {
        // TODO: Prevent
        onClose();
    };

    const handleApplySlotToEdit = () => {
        if (slotToEdit != null) {
            onChangeSlots({
                ...slots,
                [slotToEdit.key]: editedSlotValue,
            });

            const slotNode = slotRefs[slotToEdit.key];
            console.log('slotNode', slotNode);
            if (slotNode != null) {
                slotNode.textContent = editedSlotValue;
            }
        }
        handleCloseSlotToEdit();
    };

    const handleCloseSlotToEdit = () => {
        setShowSlotToEdit(false);
        setTimeout(() => {
            setSlotToEdit(null);
        }, 300);
    };

    return (
        <>
            <Dialog
                {...rest}
                onClose={handleClose}
                fullWidth
                maxWidth="xl"
            >
                <DialogTitleWithClose
                    onClose={handleClose}
                >
                    {label}
                </DialogTitleWithClose>

                <DialogContent
                    sx={{
                        display: 'flex',
                        justifyContent: 'center',
                    }}
                >
                    <Box
                        sx={{
                            width: '210mm',
                            aspectRatio: '210/297',
                            overflowY: 'scroll',
                            m: 2,
                            border: '1px solid',
                        }}
                    >

                        {
                            originalHeader != null &&
                            <Box
                                onLoad={handleIframeLoad}
                                component="iframe"
                                title={`${label} Vorschau`}
                                srcDoc={originalHeader ?? ''}
                                sandbox="allow-same-origin"
                                sx={{
                                    width: '100%',
                                    border: 'none',
                                }}
                            />
                        }

                        {
                            originalContent != null &&
                            <Box
                                onLoad={handleIframeLoad}
                                component="iframe"
                                title={`${label} Vorschau`}
                                srcDoc={originalContent ?? ''}
                                sandbox="allow-same-origin"
                                sx={{
                                    width: '100%',
                                    minHeight: '100%',
                                    border: 'none',
                                    p: '2cm',
                                }}
                            />
                        }

                        {
                            originalFooter != null &&
                            <Box
                                onLoad={handleIframeLoad}
                                component="iframe"
                                title={`${label} Vorschau`}
                                srcDoc={originalFooter ?? ''}
                                sandbox="allow-same-origin"
                                sx={{
                                    width: '100%',
                                    border: 'none',
                                }}
                            />
                        }

                    </Box>
                </DialogContent>

                <DialogActions
                    sx={{
                        borderTop: '1px solid',
                        borderColor: 'divider',
                        pt: 2,
                    }}
                >
                    <Button
                        onClick={handleClose}
                    >
                        Schließen
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog
                open={showSlotToEdit}
                onClose={handleCloseSlotToEdit}
                fullWidth
                maxWidth="lg"
            >
                <DialogTitleWithClose
                    onClose={handleCloseSlotToEdit}
                >
                    {slotToEdit?.label}
                </DialogTitleWithClose>

                <DialogContent>
                    {
                        slotToEdit != null &&
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            gutterBottom
                        >
                            {slotToEdit.hint}
                        </Typography>
                    }

                    {
                        slotToEdit != null &&
                        <Box sx={{mt: 2}}>
                            {
                                slotToEdit.type == 'text' &&
                                <TextFieldComponent
                                    label={slotToEdit.label}
                                    hint={slotToEdit.hint}
                                    value={editedSlotValue ?? slots[slotToEdit.key] ?? (isStringNotNullOrEmpty(slotToEdit.defaultValue) ? slotToEdit.defaultValue : null)}
                                    onChange={(val) => {
                                        setEditedSlotValue(val);
                                    }}
                                />
                            }

                            {
                                slotToEdit.type == 'richtext' &&
                                <RichTextInputComponent
                                    label={slotToEdit.label}
                                    hint={slotToEdit.hint}
                                    value={editedSlotValue ?? slots[slotToEdit.key] ?? (isStringNotNullOrEmpty(slotToEdit.defaultValue) ? slotToEdit.defaultValue : null)}
                                    onChange={(val) => {
                                        setEditedSlotValue(val);
                                    }}
                                />
                            }

                            {
                                slotToEdit.type == 'image' &&
                                <ImageSelector
                                    label={slotToEdit.label}
                                    hint={slotToEdit.hint}
                                    value={editedSlotValue ?? slots[slotToEdit.key] ?? (isStringNotNullOrEmpty(slotToEdit.defaultValue) ? slotToEdit.defaultValue : null)}
                                    onChange={(val) => {
                                        setEditedSlotValue(val);
                                    }}
                                    selectLabel="Datei auswählen"
                                    size={{
                                        aspectRatio: 1
                                    }}
                                />
                            }
                        </Box>
                    }
                </DialogContent>

                <DialogActions>
                    <Button
                        onClick={handleApplySlotToEdit}
                    >
                        Übernehmen
                    </Button>
                    <Button
                        onClick={handleCloseSlotToEdit}
                    >
                        Abbrechen
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}


interface HtmlTemplateEditorDialogContentProps {
    label: string;
    disabled?: boolean;
    isLoadingTemplate: boolean;
    templateLoadError: string | null;
    parsedTemplate: ReturnType<typeof parseHtmlTemplateSlots> | null;
    previewHtml: string | null;
    slots: Record<string, string | null>;
    onSlotChange: (slotId: string, value: string | null) => void;
}

function HtmlTemplateEditorDialogContent(props: HtmlTemplateEditorDialogContentProps) {
    const {
        label,
        disabled,
        isLoadingTemplate,
        templateLoadError,
        parsedTemplate,
        previewHtml,
        slots,
        onSlotChange,
    } = props;

    return (
        <Stack spacing={2}>
            {
                isLoadingTemplate &&
                <Stack
                    direction="row"
                    spacing={1.5}
                    alignItems="center"
                    sx={{py: 1}}
                >
                    <CircularProgress size={18}/>
                    <Typography
                        variant="body2"
                        color="text.secondary"
                    >
                        HTML-Vorlage wird geladen...
                    </Typography>
                </Stack>
            }

            {
                templateLoadError != null &&
                <Alert severity="error">
                    {templateLoadError}
                </Alert>
            }

            {
                parsedTemplate != null &&
                parsedTemplate.unsupportedSlots.length > 0 &&
                <Alert severity="warning">
                    Nicht unterstützte Slots: {parsedTemplate.unsupportedSlots.map((slot) => slot.id).join(', ')}
                </Alert>
            }

            {
                parsedTemplate != null &&
                !isLoadingTemplate &&
                <Grid
                    container
                    spacing={2}
                >
                    <Grid size={{xs: 12, lg: 7}}>
                        <Box
                            sx={{
                                border: '1px solid',
                                borderColor: 'divider',
                                borderRadius: 1,
                                overflow: 'hidden',
                                bgcolor: 'background.paper',
                                aspectRatio: '210/297',
                                p: 4,
                            }}
                        >
                            <Box
                                component="iframe"
                                title={`${label} Vorschau`}
                                srcDoc={previewHtml ?? ''}
                                sandbox=""
                                sx={{
                                    display: 'block',
                                    width: '100%',
                                    height: {
                                        xs: 420,
                                        lg: 620,
                                    },
                                    border: 0,
                                    bgcolor: 'white',
                                }}
                            />
                        </Box>
                    </Grid>

                    <Grid size={{xs: 12, lg: 5}}>
                        <Stack spacing={2}>
                            {
                                parsedTemplate.slots.length === 0 ?
                                    <Alert severity="info">
                                        Die ausgewählte HTML-Vorlage enthält keine unterstützten Slots.
                                    </Alert> :
                                    parsedTemplate.slots.map((slot) => (
                                        <HtmlTemplateSlotInput
                                            key={slot.id}
                                            slot={slot}
                                            value={slots[slot.id] ?? null}
                                            disabled={disabled}
                                            onChange={(nextSlotValue) => {
                                                onSlotChange(slot.id, nextSlotValue);
                                            }}
                                        />
                                    ))
                            }
                        </Stack>
                    </Grid>
                </Grid>
            }
        </Stack>
    );
}

interface HtmlTemplateSlotInputProps {
    slot: HtmlTemplateSlot;
    value: string | null;
    disabled?: boolean;
    onChange: (value: string | null) => void;
}

function HtmlTemplateSlotInput(props: HtmlTemplateSlotInputProps) {
    const {
        slot,
        value,
        disabled,
        onChange,
    } = props;

    if (slot.type === 'image') {
        return (
            <ImageSelector
                label={slot.label}
                hint=""
                selectLabel={`${slot.label} auswählen`}
                size={{aspectRatio: 3}}
                value={value}
                onChange={onChange}
                disabled={disabled}
            />
        );
    }

    if (slot.type === 'richtext') {
        return (
            <RichTextInputComponent
                label={slot.label}
                value={value}
                onChange={onChange}
                disabled={disabled}
                reducedMode
            />
        );
    }

    return (
        <TextFieldComponent
            label={slot.label}
            value={value}
            onChange={onChange}
            disabled={disabled}
            placeholder={slot.defaultValue ?? undefined}
        />
    );
}
