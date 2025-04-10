import {AnyElement} from '../models/elements/any-element';

export function elementToTypeDefinition(element: AnyElement): string {
    const lines = ['interface CurrentElement {'];
    for (const e of Object.keys(element)) {
        lines.push(`    ${e}: ${typeof [element.type]};`);
    }
    lines.push('};');
    return lines.join('\n');
}