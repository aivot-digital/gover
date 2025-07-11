import {SxProps} from '@mui/material';
import {AlertComponentProps} from '../alert/alert-component-props';
import {editor} from 'monaco-editor';

export interface CodeEditorProps {
    label?: string;
    value?: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    height?: string;
    sx?: SxProps;
    language?: string;
    typeHints?: {
        name: string;
        content: string;
    }[];
    alert?: AlertComponentProps;
    onEditorMount?: (editor: editor.IStandaloneCodeEditor) => void;
}