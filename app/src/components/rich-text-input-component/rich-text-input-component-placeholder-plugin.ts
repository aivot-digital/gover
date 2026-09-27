import {
    addToMarkdownExtension$,
    realmPlugin,
} from "@mdxeditor/editor";
import type {
    Info,
    Options as ToMarkdownExtension,
    State,
} from "mdast-util-to-markdown";

type PlaceholderKind = "doubleCurly" | "percent" | "hash" | "bang";

interface PlaceholderDelimiter {
    close: string;
    kind: PlaceholderKind;
    open: string;
}

interface TextSegment {
    type: "plain" | "placeholder";
    value: string;
}

const PLACEHOLDER_DELIMITERS: Record<PlaceholderKind, PlaceholderDelimiter> = {
    doubleCurly: {
        close: "}}",
        kind: "doubleCurly",
        open: "{{",
    },
    percent: {
        close: "%}",
        kind: "percent",
        open: "{%",
    },
    hash: {
        close: "#}",
        kind: "hash",
        open: "{#",
    },
    bang: {
        close: "!}",
        kind: "bang",
        open: "{!",
    },
};

function getPlaceholderKindByOpener(value: string, index: number): PlaceholderKind | null {
    if (value[index] !== "{") {
        return null;
    }

    switch (value[index + 1]) {
        case "{":
            return "doubleCurly";
        case "%":
            return "percent";
        case "#":
            return "hash";
        case "!":
            return "bang";
        default:
            return null;
    }
}

function findPlaceholderEnd(value: string, startIndex: number): number | null {
    const rootKind = getPlaceholderKindByOpener(value, startIndex);

    if (rootKind == null) {
        return null;
    }

    const stack: PlaceholderKind[] = [rootKind];
    let index = startIndex + 2;

    while (index < value.length) {
        const nestedKind = getPlaceholderKindByOpener(value, index);

        if (nestedKind != null) {
            stack.push(nestedKind);
            index += 2;
            continue;
        }

        const currentKind = stack[stack.length - 1];
        const currentDelimiter = PLACEHOLDER_DELIMITERS[currentKind];

        if (value.startsWith(currentDelimiter.close, index)) {
            stack.pop();
            index += currentDelimiter.close.length;

            if (stack.length === 0) {
                return index;
            }

            continue;
        }

        index += 1;
    }

    return null;
}

function splitTextSegments(value: string): TextSegment[] {
    const segments: TextSegment[] = [];
    let plainStart = 0;
    let index = 0;

    while (index < value.length) {
        const endIndex = findPlaceholderEnd(value, index);

        if (endIndex == null) {
            index += 1;
            continue;
        }

        if (plainStart < index) {
            segments.push({
                type: "plain",
                value: value.slice(plainStart, index),
            });
        }

        segments.push({
            type: "placeholder",
            value: value.slice(index, endIndex),
        });

        index = endIndex;
        plainStart = endIndex;
    }

    if (plainStart < value.length) {
        segments.push({
            type: "plain",
            value: value.slice(plainStart),
        });
    }

    return segments;
}

function serializeTextWithPlaceholders(value: string, state: State, info: Info): string {
    const segments = splitTextSegments(value);

    if (segments.length === 0) {
        return "";
    }

    if (segments.length === 1 && segments[0].type === "plain") {
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
        const serializedSegment = segment.type === "placeholder"
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

    return serializedSegments.join("");
}

const placeholderToMarkdownExtension: ToMarkdownExtension = {
    handlers: {
        text(node, _parent, state, info) {
            return serializeTextWithPlaceholders(node.value, state, info);
        },
    },
};

export const richTextInputComponentPlaceholderPlugin = realmPlugin({
    init(realm) {
        realm.pub(addToMarkdownExtension$, placeholderToMarkdownExtension);
    },
});

export const placeholderPlugin = richTextInputComponentPlaceholderPlugin;
