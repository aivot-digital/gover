import {
    addSyntaxExtension$,
    createRootEditorSubscription$,
    realmPlugin,
} from '@mdxeditor/editor';
import {
    COMMAND_PRIORITY_CRITICAL,
    FORMAT_TEXT_COMMAND,
    mergeRegister,
    type TextFormatType,
    TextNode,
} from 'lexical';
import type {Extension} from 'micromark-util-types';

// The Markdown renderers have no syntax for these formats, so mails, PDFs and the frontend would show the tags MDXEditor
// writes for them, such as <u>, as text. The editor therefore never applies them.
const UNSUPPORTED_FORMATS: readonly TextFormatType[] = ['underline', 'subscript', 'superscript', 'highlight'];

// MDXEditor reads ==text== as highlighted text, while the renderers keep the equals signs.
const disabledHighlightSyntaxExtension: Extension = {
    disable: {null: ['highlight']},
};

export const supportedTextFormatsPlugin = realmPlugin({
    init(realm) {
        realm.pub(addSyntaxExtension$, disabledHighlightSyntaxExtension);
        realm.pub(createRootEditorSubscription$, (editor) => mergeRegister(
            // Covers keyboard shortcuts such as Ctrl+U and the formatting commands of the browser.
            editor.registerCommand(
                FORMAT_TEXT_COMMAND,
                (format) => UNSUPPORTED_FORMATS.includes(format),
                COMMAND_PRIORITY_CRITICAL,
            ),
            // Covers pasted text, for example underlined text copied from a word processor.
            editor.registerNodeTransform(TextNode, (node) => {
                for (const format of UNSUPPORTED_FORMATS) {
                    if (node.hasFormat(format)) {
                        node.toggleFormat(format);
                    }
                }
            }),
        ));
    },
});
