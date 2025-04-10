import {EditorContent, useEditor} from '@tiptap/react';
import Document from '@tiptap/extension-document';
import Bold from '@tiptap/extension-bold';
import Italic from '@tiptap/extension-italic';
import Strike from '@tiptap/extension-strike';
import Paragraph from '@tiptap/extension-paragraph';
import ListItem from '@tiptap/extension-list-item';
import BulletList from '@tiptap/extension-bullet-list';
import OrderedList from '@tiptap/extension-ordered-list';
import Link from '@tiptap/extension-link';
import Text from '@tiptap/extension-text';
import React, {useEffect} from 'react';
import {Box, Paper, Typography, useEventCallback} from '@mui/material';
import {RichTextEditorMenuComponentView} from '../rich-text-editor-menu.component.view';
import './rich-text-editor.component.scss';

interface RichTextEditorComponentViewProps {
    label?: string;
    value: string;
    onChange: (text: string | undefined) => void;
    required?: boolean;
    error?: string;
    disabled?: boolean;
    hint?: string;
}

export function RichTextEditorComponentView(props: RichTextEditorComponentViewProps): JSX.Element {
    const onChangeCallback = useEventCallback(({editor}: any) => {
        if (editor.isEmpty) {
            props.onChange(undefined);
        } else {
            props.onChange(editor.getHTML());
        }
    });

    const editor = useEditor({
        extensions: [
            Document,
            Paragraph.configure({
                HTMLAttributes: {
                    class: 'MuiTypography-root MuiTypography-body2',
                },
            }),
            ListItem,
            BulletList,
            OrderedList,
            Link.configure({
                openOnClick: false,
                autolink: true,
            }),
            Text,
            Bold,
            Italic,
            Strike,
        ],
        onUpdate: onChangeCallback,
        editable: !(props.disabled ?? false),
    });

    useEffect(() => {
        if (editor != null && editor.isEmpty) {
            editor.commands.setContent(props.value ?? '');
        }
    }, [props.value, editor]);

    return (
        <Box
        sx={{
            mt: 2,
            mb: 1,
            cursor: (props.disabled ?? false) ? 'not-allowed' : undefined,
        }}>
            {
                props.label != null &&
                <Typography
                    sx={{
                        mb: 2,
                    }}
                >
                    {props.label}{props.required === true ? ' *' : ''}
                </Typography>
            }

            {
                !(props.disabled ?? false) &&
                <RichTextEditorMenuComponentView editor={editor}/>
            }

            <Paper
                elevation={0}
                sx={{
                    mt: .75,
                    py: 1,
                    px: 2,
                    backgroundColor: (props.disabled ?? false) ? '#F8F8F8' : undefined,
                    cursor: (props.disabled ?? false) ? 'not-allowed' : 'text',
                    border: '1px solid rgba(0, 0, 0, 0.23)',
                    '&:hover': (props.disabled ?? false) ? {} : {border: '1px solid rgba(0, 0, 0, 0.87)'},
                    '&:focus-within': {borderColor: 'var(--hw-primary)'},
                }}
                className="editorWrapper"
                onClick={() => {
                    if (editor != null && !editor.isFocused) {
                        editor.commands.focus();
                    }
                }}
            >
                <EditorContent
                    editor={editor}
                    style={{
                        backgroundColor: (props.disabled ?? false) ? '#F8F8F8' : undefined,
                    }}
                />
            </Paper>

            {
                (
                    props.hint != null ||
                    props.error != null
                ) &&
                <Typography
                    variant="caption"
                    color={props.error != null ? 'error' : undefined}
                >
                    {props.error ?? props.hint}
                </Typography>
            }
        </Box>
    );
}
