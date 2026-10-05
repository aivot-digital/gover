import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export enum CodeInputFieldLanguage {
    Javascript = 'javascript',
    Typescript = 'typescript',
    Json = 'json',
    Html = 'html',
    Css = 'css',
    Markdown = 'markdown',
}

export interface CodeInputElement extends BaseInputElement<ElementType.CodeInput> {
    language: CodeInputFieldLanguage | null | undefined;
    editorHeight: number | null | undefined;
    wordWrap: boolean | null | undefined;
}
