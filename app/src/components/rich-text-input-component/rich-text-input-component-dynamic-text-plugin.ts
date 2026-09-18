import {
    addComposerChild$,
    addToMarkdownExtension$,
    realmPlugin,
} from '@mdxeditor/editor';
import type {
    Info,
    Options as ToMarkdownExtension,
    State,
} from 'mdast-util-to-markdown';
import {DynamicTextLexicalPlugin} from '../dynamic-text/dynamic-text-lexical';
import {splitDynamicTextSegments} from '../dynamic-text/dynamic-text-syntax';

function serializeDynamicText(value: string, state: State, info: Info): string {
    const segments = splitDynamicTextSegments(value);
    if (segments.length === 0) {
        return '';
    }
    if (segments.length === 1 && segments[0].type === 'plain') {
        return state.safe(value, info);
    }

    const tracker = state.createTracker(info);
    const serializedSegments: string[] = [];
    let before = info.before;

    for (let index = 0; index < segments.length; index += 1) {
        const segment = segments[index];
        const after = index + 1 < segments.length
            ? segments[index + 1].value.charAt(0)
            : info.after;
        const serializedSegment = segment.type === 'token'
            ? segment.value
            : state.safe(segment.value, {
                ...tracker.current(),
                after,
                before,
            });

        serializedSegments.push(serializedSegment);
        tracker.move(serializedSegment);
        if (serializedSegment.length > 0) {
            before = serializedSegment.slice(-1);
        }
    }

    return serializedSegments.join('');
}

const dynamicTextToMarkdownExtension: ToMarkdownExtension = {
    handlers: {
        text(node, _parent, state, info) {
            return serializeDynamicText(node.value, state, info);
        },
    },
};

interface DynamicTextPluginParams {
    highlight?: boolean;
}

export const dynamicTextPlugin = realmPlugin<DynamicTextPluginParams>({
    init(realm, params) {
        realm.pub(addToMarkdownExtension$, dynamicTextToMarkdownExtension);
        if (params?.highlight) {
            realm.pub(addComposerChild$, DynamicTextLexicalPlugin);
        }
    },
});
