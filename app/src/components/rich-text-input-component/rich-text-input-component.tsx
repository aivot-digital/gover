import Typography from "@mui/material/Typography";
import {Box, SxProps} from "@mui/material";
import {alpha, useTheme} from "@mui/material/styles";
import {useCallback, useState} from "react";
import {
    BlockTypeSelect,
    BoldItalicUnderlineToggles,
    CodeToggle,
    CreateLink,
    headingsPlugin,
    listsPlugin,
    ListsToggle,
    markdownShortcutPlugin,
    MDXEditor,
    quotePlugin,
    toolbarPlugin,
    UndoRedo,
    type Translation
} from "@mdxeditor/editor";
import {isStringNullOrEmpty} from "../../utils/string-utils";
import '@mdxeditor/editor/style.css';

const MDX_EDITOR_DE_TRANSLATIONS: Record<string, string> = {
    'toolbar.undo': 'Rückgängig {{shortcut}}',
    'toolbar.redo': 'Wiederholen {{shortcut}}',
    'toolbar.bold': 'Fett',
    'toolbar.removeBold': 'Fett entfernen',
    'toolbar.italic': 'Kursiv',
    'toolbar.removeItalic': 'Kursiv entfernen',
    'toolbar.underline': 'Unterstreichen',
    'toolbar.removeUnderline': 'Unterstreichung entfernen',
    'toolbar.inlineCode': 'Inline-Code',
    'toolbar.removeInlineCode': 'Inline-Code entfernen',
    'toolbar.link': 'Link erstellen',
    'toolbar.bulletedList': 'Aufzählung',
    'toolbar.numberedList': 'Nummerierte Liste',
    'toolbar.checkList': 'Checkliste',
    'toolbar.blockTypes.paragraph': 'Absatz',
    'toolbar.blockTypes.quote': 'Zitat',
    'toolbar.blockTypes.heading': 'Überschrift {{level}}',
    'toolbar.blockTypeSelect.selectBlockTypeTooltip': 'Blocktyp auswählen',
    'toolbar.blockTypeSelect.placeholder': 'Blocktyp',
    'createLink.url': 'URL',
    'createLink.urlPlaceholder': 'URL auswählen oder einfügen',
    'createLink.textTooltip': 'Der anzuzeigende Linktext',
    'createLink.text': 'Linktext',
    'createLink.titleTooltip': 'Der Title-Attribut-Text des Links',
    'createLink.title': 'Linktitel',
    'createLink.saveTooltip': 'URL übernehmen',
    'createLink.cancelTooltip': 'Änderung verwerfen',
    'dialogControls.save': 'Speichern',
    'dialogControls.cancel': 'Abbrechen',
    'linkPreview.open': '{{url}} in neuem Fenster öffnen',
    'linkPreview.edit': 'Link-URL bearbeiten',
    'linkPreview.copyToClipboard': 'In Zwischenablage kopieren',
    'linkPreview.copied': 'Kopiert!',
    'linkPreview.remove': 'Link entfernen',
};

const mdxEditorGermanTranslation: Translation = (key, defaultValue, interpolations) => {
    const template = MDX_EDITOR_DE_TRANSLATIONS[key] ?? defaultValue;
    return template.replace(/\{\{\s*([^{}\s]+)\s*}}/g, (_, token: string) => String(interpolations?.[token] ?? ''));
};

interface RichTextInputComponentProps {
    label: string;
    hint?: string | null | undefined;
    error?: string | null | undefined;
    required?: boolean | null | undefined;
    disabled?: boolean | null | undefined;
    readOnly?: boolean | null | undefined;
    value: string | null | undefined;
    onChange: (value: string | null) => void;
    sx?: SxProps | null | undefined;
}

export function RichTextInputComponent(props: RichTextInputComponentProps) {
    const theme = useTheme();
    const [overlayContainer, setOverlayContainer] = useState<HTMLElement | null>(null);
    const {
        label,
        hint,
        error,
        required,
        disabled,
        readOnly,
        value,
        onChange,
        sx,
    } = props;

    const isReadOnly = Boolean(disabled) || Boolean(readOnly);
    const sxArray = Array.isArray(sx) ? sx : [sx];
    const focusColor = error != null ? theme.palette.error.main : theme.palette.primary.main;
    const baseBg = isReadOnly
        ? theme.palette.action.disabledBackground
        : theme.palette.background.paper;
    const toolbarBg = isReadOnly
        ? theme.palette.action.hover
        : alpha(theme.palette.text.primary, 0.035);
    const buttonHoverBg = alpha(theme.palette.text.primary, 0.08);
    const buttonActiveBg = alpha(theme.palette.text.primary, 0.12);
    const accentBg = alpha(theme.palette.primary.main, 0.14);
    const handleOverlayContainerRef = useCallback((node: HTMLDivElement | null) => {
        setOverlayContainer(node);
    }, []);

    return (
        <Box
            sx={[
                {
                    opacity: disabled ? 0.65 : 1,
                },
                ...sxArray,
            ]}
        >
            <Typography
                sx={{
                    marginBottom: 1,
                    fontWeight: 'medium',
                }}
            >
                {label}{required ? ' *' : ''}
            </Typography>

            <Box
                ref={handleOverlayContainerRef}
                sx={{
                    position: 'relative',
                    border: '1px solid',
                    borderColor: error != null ? 'error.main' : 'divider',
                    borderRadius: 1,
                    backgroundColor: baseBg,
                    transition: theme.transitions.create(['border-color'], {
                        duration: theme.transitions.duration.shorter,
                    }),
                    '&::after': {
                        content: '""',
                        position: 'absolute',
                        inset: -1,
                        border: '2px solid transparent',
                        borderRadius: 'inherit',
                        pointerEvents: 'none',
                        transition: theme.transitions.create(['border-color'], {
                            duration: theme.transitions.duration.shorter,
                        }),
                    },
                    '&:focus-within': {
                        borderColor: focusColor,
                    },
                    '&:focus-within::after': {
                        borderColor: focusColor,
                    },
                    '& .gover-mdx-editor': {
                        '--font-body': theme.typography.fontFamily,
                        '--basePageBg': baseBg,
                        '--baseBase': baseBg,
                        '--baseBgSubtle': alpha(theme.palette.text.primary, 0.025),
                        '--baseBg': toolbarBg,
                        '--baseBgHover': buttonHoverBg,
                        '--baseBgActive': buttonActiveBg,
                        '--baseLine': theme.palette.divider,
                        '--baseBorder': theme.palette.divider,
                        '--baseBorderHover': theme.palette.text.disabled,
                        '--baseSolid': theme.palette.text.secondary,
                        '--baseSolidHover': theme.palette.text.primary,
                        '--baseText': theme.palette.text.secondary,
                        '--baseTextContrast': theme.palette.text.primary,
                        '--accentBgSubtle': alpha(theme.palette.primary.main, 0.08),
                        '--accentBg': accentBg,
                        '--accentBgHover': alpha(theme.palette.primary.main, 0.18),
                        '--accentBgActive': alpha(theme.palette.primary.main, 0.24),
                        '--accentLine': alpha(theme.palette.primary.main, 0.3),
                        '--accentBorder': alpha(theme.palette.primary.main, 0.4),
                        '--accentBorderHover': alpha(theme.palette.primary.main, 0.55),
                        '--accentSolid': theme.palette.primary.main,
                        '--accentSolidHover': theme.palette.primary.dark,
                        '--accentText': theme.palette.primary.main,
                        '--accentTextContrast': theme.palette.primary.contrastText,
                        fontFamily: 'inherit',
                    },
                    '& .gover-mdx-editor [class*="_toolbarRoot_"]': {
                        borderBottom: '1px solid',
                        borderColor: error != null ? alpha(theme.palette.error.main, 0.3) : 'divider',
                        borderRadius: '4px 4px 0 0',
                        paddingInline: 1,
                        paddingBlock: 0.75,
                        minHeight: 46,
                        gap: 0.5,
                    },
                    '& .gover-mdx-editor [class*="_toolbarRoot_"] [role="separator"]': {
                        marginInline: 0.5,
                        marginBlock: 0.25,
                    },
                    '& .gover-mdx-editor [class*="_toolbarToggleItem_"], & .gover-mdx-editor [class*="_toolbarButton_"]': {
                        borderRadius: 1,
                        padding: 0.5,
                        transition: theme.transitions.create(['background-color', 'color'], {
                            duration: theme.transitions.duration.shortest,
                        }),
                    },
                    '& .gover-mdx-editor [class*="_toolbarToggleItem_"]:hover, & .gover-mdx-editor [class*="_toolbarButton_"]:hover': {
                        backgroundColor: buttonHoverBg,
                    },
                    '& .gover-mdx-editor [class*="_toolbarToggleItem_"][data-state="on"], & .gover-mdx-editor [class*="_toolbarButton_"][data-state="on"]': {
                        backgroundColor: accentBg,
                        color: 'primary.main',
                    },
                    '& .gover-mdx-editor [class*="_selectTrigger_"]': {
                        borderRadius: 1,
                        border: '1px solid',
                        borderColor: 'divider',
                        minHeight: 30,
                        backgroundColor: baseBg,
                    },
                    '& .gover-mdx-editor [class*="_selectTrigger_"][data-state="open"]': {
                        borderColor: 'primary.main',
                    },
                    '& [class*="_toolbarNodeKindSelectContainer_"], & [class*="_toolbarButtonDropdownContainer_"], & [class*="_selectContainer_"], & [class*="_linkDialogPopoverContent_"], & [class*="_tableColumnEditorPopoverContent_"], & [class*="_dialogContent_"], & [class*="_largeDialogContent_"], & [class*="_popoverContent_"]': {
                        backgroundColor: 'background.paper',
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1,
                        boxShadow: theme.shadows[3],
                        filter: 'none',
                        padding: 0.5,
                    },
                    '& [class*="_toolbarNodeKindSelectContainer_"], & [class*="_toolbarButtonDropdownContainer_"], & [class*="_selectContainer_"]': {
                        padding: 0.25,
                    },
                    '& [class*="_toolbarNodeKindSelectItem_"], & [class*="_selectItem_"]': {
                        borderRadius: 0.75,
                        marginBottom: 0.25,
                        paddingBlock: 1,
                        paddingInline: 1.25,
                        transition: theme.transitions.create(['background-color', 'color'], {
                            duration: theme.transitions.duration.shortest,
                        }),
                    },
                    '& [class*="_toolbarNodeKindSelectItem_"][data-highlighted], & [class*="_selectItem_"][data-highlighted]': {
                        backgroundColor: 'action.hover',
                    },
                    '& [class*="_toolbarNodeKindSelectItem_"][data-state="checked"], & [class*="_selectItem_"][data-state="checked"]': {
                        backgroundColor: alpha(theme.palette.primary.main, 0.1),
                        color: 'text.primary',
                        fontWeight: 600,
                    },
                    '& [class*="_dialogInput_"], & [class*="_linkDialogInput_"], & [class*="_downshiftInput_"], & [class*="_textInput_"]': {
                        fontFamily: 'inherit',
                        color: 'text.primary',
                    },
                    '& [class*="_dialogInput_"]::placeholder, & [class*="_linkDialogInput_"]::placeholder, & [class*="_downshiftInput_"]::placeholder': {
                        color: theme.palette.text.disabled,
                    },
                    '& [class*="_linkDialogInputWrapper_"], & [class*="_downshiftInputWrapper_"]': {
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1,
                        backgroundColor: 'background.paper',
                    },
                    '& [class*="_linkDialogInputWrapper_"]:focus-within, & [class*="_downshiftInputWrapper_"]:focus-within': {
                        borderColor: 'primary.main',
                        boxShadow: `0 0 0 1px ${theme.palette.primary.main}`,
                    },
                    '& .gover-mdx-editor [class*="_contentEditable_"]': {
                        minHeight: 180,
                        maxHeight: '50vh',
                        overflowY: 'auto',
                        padding: theme.spacing(1.5, 2),
                        fontSize: theme.typography.body1.fontSize,
                        lineHeight: 1.6,
                        color: 'text.primary',
                    },
                    '& .gover-mdx-editor [class*="_placeholder_"]': {
                        color: 'text.disabled',
                    },
                    '& .gover-mdx-editor [class*="_contentEditable_"] p:first-of-type': {
                        marginTop: 0,
                    },
                    '& .gover-mdx-editor [class*="_contentEditable_"] p:last-of-type': {
                        marginBottom: 0,
                    },
                    '& .gover-mdx-editor [class*="_contentEditable_"] blockquote': {
                        borderLeft: `3px solid ${theme.palette.divider}`,
                        paddingLeft: theme.spacing(1.5),
                        marginLeft: 0,
                        color: theme.palette.text.secondary,
                    },
                    '& .gover-mdx-editor [class*="_contentEditable_"] code': {
                        backgroundColor: alpha(theme.palette.text.primary, 0.08),
                        borderRadius: 1,
                        padding: theme.spacing(0.1, 0.5),
                    },
                    '& .gover-mdx-editor [class*="_contentEditable_"] a': {
                        color: 'primary.main',
                        textDecorationColor: alpha(theme.palette.primary.main, 0.35),
                    },
                    ...(disabled
                        ? {
                            '&:focus-within': {
                                borderColor: error != null ? 'error.main' : 'divider',
                            },
                            '&:focus-within::after': {
                                borderColor: 'transparent',
                            },
                            '& .gover-mdx-editor': {
                                pointerEvents: 'none',
                            },
                        }
                        : {}),
                }}
            >
                <MDXEditor
                    className="gover-mdx-editor"
                    overlayContainer={overlayContainer}
                    translation={mdxEditorGermanTranslation}
                    markdown={value ?? ''}
                    readOnly={isReadOnly}
                    onChange={(val) => {
                        if (isStringNullOrEmpty(val)) {
                            onChange(null);
                            return;
                        }
                        onChange(val ?? null);
                    }}
                    plugins={[
                        headingsPlugin(),
                        quotePlugin(),
                        listsPlugin(),
                        toolbarPlugin({
                            toolbarContents: () => (
                                <>
                                    <UndoRedo/>
                                    <BoldItalicUnderlineToggles/>
                                    <BlockTypeSelect/>
                                    <CodeToggle/>
                                    <CreateLink/>
                                    <ListsToggle/>
                                </>
                            )
                        }),
                        markdownShortcutPlugin(),
                    ]}
                />
            </Box>

            {
                (error != null || hint != null) &&
                <Typography
                    sx={{
                        marginTop: 1,
                        color: error != null ? 'error.main' : 'text.secondary',
                    }}
                    variant="caption"
                >
                    {error ?? hint}
                </Typography>
            }
        </Box>
    );
}
