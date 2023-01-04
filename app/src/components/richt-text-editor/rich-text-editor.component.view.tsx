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
import {Paper, Typography, useEventCallback} from '@mui/material';
import {RichTextEditorMenuComponentView} from './components/rich-text-editor-menu.component.view';
import './rich-text-editor.component.scss';

interface RichTextEditorComponentViewProps {
    label?: string;
    value: string;
    onChange: (text: string) => void;
}

export function RichTextEditorComponentView({value, onChange, label}: RichTextEditorComponentViewProps) {
    const onChangeCallback = useEventCallback(({editor}) => {
        if (editor.isEmpty) {
            onChange('');
        } else {
            onChange(editor.getHTML());
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
    });

    useEffect(() => {
        if (editor != null && editor.isEmpty) {
            editor.commands.setContent(value ?? '');
        }
    }, [value, editor]);

    return (
        <>
            {
                label &&
                <Typography sx={{mb: 2}}>
                    {label}
                </Typography>
            }
            <RichTextEditorMenuComponentView editor={editor}/>
            <Paper
                elevation={0}
                sx={{mt: 2, py: 1, px: 2}}
                className="editorWrapper"
                onClick={() => {
                    if (editor != null && !editor.isFocused) {
                        editor.commands.focus();
                    }
                }}
            >
                <EditorContent editor={editor}/>
            </Paper>
        </>
    );
}
