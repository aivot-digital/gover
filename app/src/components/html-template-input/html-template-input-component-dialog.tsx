import {ReactEventHandler, useCallback, useEffect, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography, useTheme} from '@mui/material';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {renderToStaticMarkup} from 'react-dom/server';
import {ImageSelector} from '../../modules/assets/components/image-selector';
import {AssetsApiService} from '../../modules/assets/assets-api-service';
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

type SlotType = 'text' | 'richtext' | 'image';

interface SlotToEdit {
    key: string;
    type: SlotType;
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
    const [editedSlotValue, setEditedSlotValue] = useState<string | null | undefined>(undefined);

    useEffect(() => {
        // Reset the edited slot value if the slot to edit changes to prevent old value display.
        setEditedSlotValue(undefined);
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
                    setSlotContent(elem, slots[slotKey]);
                }

                elem.style.backgroundColor = theme.palette.grey[200];
                elem.style.cursor = 'pointer';
                elem.onclick = () => {
                    const type: SlotType = elem.dataset.slotType as SlotType ?? 'text';
                    let defaultValue = '';
                    if (type === 'text') {
                        defaultValue = collapseWhiteSpacesInText(elem.textContent ?? '');
                    } else if (type === 'richtext') {
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
    }, [slots]);

    const handleClose = () => {
        // TODO: Prevent
        onClose();
    };

    const handleApplySlotToEdit = () => {
        if (slotToEdit != null) {
            onChangeSlots({
                ...slots,
                [slotToEdit.key]: editedSlotValue ?? null,
            });

            const slotNode = slotRefs[slotToEdit.key];
            if (slotNode != null) {
                setSlotContent(slotNode, editedSlotValue);
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

    const value = slotToEdit == null
        ? undefined
        : (
            editedSlotValue === undefined
                ? (
                    slots[slotToEdit.key] ?? (
                        isStringNotNullOrEmpty(slotToEdit.defaultValue)
                            ? slotToEdit.defaultValue
                            : null
                    )
                )
                : editedSlotValue
        );

    useEffect(() => {
        console.log('slots changed, updating iframes', slots);
    }, [slots]);

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
                        Übernehmen
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog
                open={showSlotToEdit}
                onClose={handleCloseSlotToEdit}
                fullWidth
                maxWidth={
                    slotToEdit?.type === 'image' ? 'sm' : 'md'
                }
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
                                    value={value}
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
                                    value={value}
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
                                    value={value != null && value.startsWith('/') ? null : (value ?? null)}
                                    onChange={(val) => {
                                        setEditedSlotValue(val);
                                    }}
                                    selectLabel="Datei auswählen"
                                    size={{
                                        aspectRatio: 1,
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

function collapseWhiteSpacesInText(text: string): string {
    return text.replace(/\s+/g, ' ').trim();
}

function setSlotContent(slotNode: HTMLElement, value: string | null | undefined): void {
    const slotType: SlotType = slotNode.dataset.slotType as SlotType ?? 'text';

    switch (slotType) {
        case 'text':
            slotNode.textContent = value ?? '';
            break;
        case 'richtext':
            slotNode.innerHTML = renderRichTextMarkdown(value ?? '');
            break;
        case 'image':
            let link = '';
            if (value != null && isStringNotNullOrEmpty(value)) {
                link = AssetsApiService.useAssetLink(value);
            }
            slotNode.setAttribute('src', link);
            break;
    }
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
