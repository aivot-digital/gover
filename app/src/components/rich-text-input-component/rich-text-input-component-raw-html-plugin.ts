import {
    addImportVisitor$,
    addSyntaxExtension$,
    type MdastImportVisitor,
    realmPlugin,
} from '@mdxeditor/editor';
import {$createTextNode} from 'lexical';
import type {Html} from 'mdast';
import type {Extension} from 'micromark-util-types';

// MDXEditor disables these CommonMark constructs while it parses HTML as MDX. Keep them disabled after turning that
// parser off, so indented lines, angle-bracket links and block-level tags are still read as ordinary paragraphs.
const disabledHtmlSyntaxExtension: Extension = {
    disable: {null: ['autolink', 'codeIndented', 'htmlFlow']},
};

// Rich text is stored by other users and shown to staff with broader permissions. Raw HTML must therefore never become
// a DOM element in the editor; it is imported as literal text instead, so it stays visible and can be removed. This
// includes the formatting tags MDXEditor would otherwise read as underline, superscript or subscript, because the
// Markdown renderers show them as text as well.
const rawHtmlAsTextVisitor: MdastImportVisitor<Html> = {
    testNode: 'html',
    // Run before the formatting visitors, which would match exact formatting tags such as <u>.
    priority: 100,
    visitNode({mdastNode, actions}) {
        const textNode = $createTextNode(mdastNode.value);
        textNode.setFormat(actions.getParentFormatting());
        actions.addAndStepInto(textNode);
    },
};

export const rawHtmlAsTextPlugin = realmPlugin({
    init(realm) {
        realm.pub(addSyntaxExtension$, disabledHtmlSyntaxExtension);
        realm.pub(addImportVisitor$, rawHtmlAsTextVisitor);
    },
});
