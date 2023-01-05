import {ElementPath} from '../../models/_lib/element-path';

export function nextPath(path?: ElementPath): ElementPath | null {
    if (!path) {
        return null;
    }
    const previous = [...path];
    let index = previous.pop() ?? 0;
    if (index > 0) {
        index++;
    }
    previous.push(index);
    return previous;
}
