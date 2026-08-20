import {type Theme} from '@mui/material';
import {type PrismTheme} from 'prism-react-renderer';

export type CodeBlockLanguage =
    | 'text'
    | 'json'
    | 'javascript'
    | 'typescript'
    | 'html'
    | 'css'
    | 'markdown'
    | 'xml';

export function createCodeSyntaxTheme(theme: Theme): PrismTheme {
    // Syntax colors stay independent of configurable brand colors, which are not guaranteed to provide sufficient
    // contrast for small tokens. Both palettes retain the familiar semantic distinction between code token roles.
    const colors = theme.palette.mode === 'dark'
        ? {
            comment: '#9BA7B4',
            punctuation: '#CBD5E1',
            property: '#FFA0AC',
            string: '#9CC4FF',
            number: '#87D39B',
            keyword: '#70D5DC',
            constant: '#E8C36A',
            function: '#D6A8FF',
        }
        : {
            comment: '#556170',
            punctuation: '#4B5563',
            property: '#8B1E5A',
            string: '#1E4E8C',
            number: '#1E6A3A',
            keyword: '#006D75',
            constant: '#7A5500',
            function: '#5B3FA3',
        };

    return {
        plain: {
            backgroundColor: 'transparent',
            color: theme.palette.text.primary,
        },
        styles: [
            {
                types: ['comment', 'prolog', 'doctype', 'cdata'],
                style: {color: colors.comment},
            },
            {
                types: ['punctuation', 'operator'],
                style: {color: colors.punctuation},
            },
            {
                types: ['property', 'tag', 'attr-name', 'selector', 'symbol', 'deleted', 'italic'],
                style: {color: colors.property},
            },
            {
                types: ['string', 'char', 'attr-value', 'inserted'],
                style: {color: colors.string},
            },
            {
                types: ['number'],
                style: {color: colors.number},
            },
            {
                types: ['boolean', 'keyword', 'builtin', 'variable', 'atrule'],
                style: {color: colors.keyword},
            },
            {
                types: ['null', 'constant', 'color'],
                style: {color: colors.constant},
            },
            {
                types: ['function', 'class-name', 'regex', 'url'],
                style: {color: colors.function},
            },
            {
                types: ['title', 'important', 'bold'],
                style: {color: colors.function, fontWeight: 'bold'},
            },
        ],
    };
}
