export type ExecutionSummaryBlock =
    | {kind: 'markdown'; markdown: string}
    | {kind: 'definitions'; entries: {term: string; markdown: string}[]};

/** Reads the definition-list syntax emitted by ExecutionSummaryMarkdown without reinterpreting legacy bold text. */
export function parseExecutionSummary(markdown: string): ExecutionSummaryBlock[] {
    const lines = markdown.replace(/\r\n?/g, '\n').split('\n');
    const blocks: ExecutionSummaryBlock[] = [];
    let pending: string[] = [];
    let fence: {character: string; length: number} | null = null;

    const flushMarkdown = () => {
        const value = pending.join('\n').replace(/^\n+|\n+$/g, '');
        if (value.trim()) blocks.push({kind: 'markdown', markdown: value});
        pending = [];
    };

    for (let index = 0; index < lines.length; index++) {
        const line = lines[index];
        const fenceMatch = line.match(/^ {0,3}(`{3,}|~{3,})(.*)$/);
        if (fence != null) {
            pending.push(line);
            if (fenceMatch?.[1][0] === fence.character && fenceMatch[1].length >= fence.length && !fenceMatch[2].trim()) {
                fence = null;
            }
            continue;
        }
        if (fenceMatch != null) {
            fence = {character: fenceMatch[1][0], length: fenceMatch[1].length};
            pending.push(line);
            continue;
        }

        const isTerm = /^[^\s>#`~*+-]/.test(line) && (index === 0 || !lines[index - 1].trim());
        if (!isTerm || !lines[index + 1]?.startsWith(':   ')) {
            pending.push(line);
            continue;
        }

        flushMarkdown();
        const value = [lines[++index].slice(4)];
        while (index + 1 < lines.length) {
            const next = lines[index + 1];
            if (next.startsWith('    ')) {
                value.push(next.slice(4));
            } else if (!next.trim()) {
                value.push('');
            } else {
                break;
            }
            index++;
        }

        const entry = {term: line, markdown: value.join('\n').replace(/^\n+|\n+$/g, '')};
        const previous = blocks[blocks.length - 1];
        if (previous?.kind === 'definitions') {
            previous.entries.push(entry);
        } else {
            blocks.push({kind: 'definitions', entries: [entry]});
        }
    }

    flushMarkdown();
    return blocks;
}
