import React, {useCallback} from 'react';
import {Editor} from '@tiptap/react';
import {Button, ButtonGroup, SvgIconTypeMap} from '@mui/material';
import FormatBoldOutlinedIcon from '@mui/icons-material/FormatBoldOutlined';
import FormatItalicOutlinedIcon from '@mui/icons-material/FormatItalicOutlined';
import StrikethroughSOutlinedIcon from '@mui/icons-material/StrikethroughSOutlined';
import FormatClearOutlinedIcon from '@mui/icons-material/FormatClearOutlined';
import FormatListBulletedOutlinedIcon from '@mui/icons-material/FormatListBulletedOutlined';
import FormatListNumberedOutlinedIcon from '@mui/icons-material/FormatListNumberedOutlined';
import InsertLinkOutlinedIcon from '@mui/icons-material/InsertLinkOutlined';
import LinkOffOutlinedIcon from '@mui/icons-material/LinkOffOutlined';
import {OverridableComponent} from "@mui/material/OverridableComponent";
import {usePrompt} from "../providers/prompt-provider";

export function RichTextEditorMenuComponentView({editor}: {editor: Editor | null}) {
    const showPrompt = usePrompt();

    const fontOptions: {
        icon: OverridableComponent<SvgIconTypeMap>;
        style?: string;
        func: () => void;
    }[] = [
        {
            icon: FormatBoldOutlinedIcon,
            style: 'bold',
            func: () => editor?.chain().focus().toggleBold().run(),
        },
        {
            icon: FormatItalicOutlinedIcon,
            style: 'italic',
            func: () => editor?.chain().focus().toggleItalic().run(),
        },
        {
            icon: StrikethroughSOutlinedIcon,
            style: 'strike',
            func: () => editor?.chain().focus().toggleStrike().run(),
        },
        {
            icon: FormatClearOutlinedIcon,
            func: () => editor?.chain().focus().unsetAllMarks().run(),
        },
    ];

    const parOptions: {
        icon: OverridableComponent<SvgIconTypeMap>;
        style: string;
        func: () => any;
    }[] = [
        {
            icon: FormatListBulletedOutlinedIcon,
            style: 'bulletList',
            func: () => editor?.chain().focus().toggleBulletList().run(),
        },
        {
            icon: FormatListNumberedOutlinedIcon,
            style: 'orderedList',
            func: () => editor?.chain().focus().toggleOrderedList().run(),
        },
    ];

    const setLink = useCallback(async () => {
        if (editor) {
            const previousUrl = editor.getAttributes('link').href;
            let url = await showPrompt({
                title: "Link bearbeiten",
                message: "Bitte geben Sie eine gültige URL für diesen Link ein:",
                inputLabel: "URL",
                inputPlaceholder: previousUrl ?? "https://..",
                confirmButtonText: "Bestätigen",
                cancelButtonText: "Abbrechen",
                defaultValue: previousUrl,
            });

            // cancelled
            if (url === null) {
                return;
            }

            // empty
            if (url === '') {
                editor.chain().focus().extendMarkRange('link').unsetLink()
                    .run();

                return;
            }

            if (!url.startsWith('http://') && !url.startsWith('https://')) {
                url = 'http://' + url;
            }

            // update link
            editor.chain().focus().extendMarkRange('link').setLink({href: url})
                .run();
        }
    }, [editor]);

    if (!editor) {
        return null;
    }

    return (
        <>
            <ButtonGroup>
                {
                    fontOptions.map((option, index) => (
                        <Button
                            key={index}
                            onClick={option.func}
                            variant={option.style != null && editor.isActive(option.style) ? 'contained' : 'outlined'}
                        >
                            <option.icon fontSize={"small"}/>
                        </Button>
                    ))
                }
            </ButtonGroup>

            <ButtonGroup sx={{ml: 1}}>
                {
                    parOptions.map((option, index) => (
                        <Button
                            key={index}
                            onClick={option.func}
                            variant={editor.isActive(option.style) ? 'contained' : 'outlined'}
                        >
                            <option.icon fontSize={"small"}/>
                        </Button>
                    ))
                }
            </ButtonGroup>

            <ButtonGroup sx={{ml: 1}}>
                <Button
                    onClick={setLink}
                    variant={editor.isActive('link') ? 'contained' : 'outlined'}
                >
                    <InsertLinkOutlinedIcon fontSize={"small"}/>
                </Button>
                <Button
                    onClick={() => editor.chain().focus().unsetLink().run()}
                    disabled={!editor.isActive('link')}
                >
                    <LinkOffOutlinedIcon fontSize={"small"}/>
                </Button>
            </ButtonGroup>
        </>
    );
}
