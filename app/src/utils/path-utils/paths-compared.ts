import {ElementPath} from '../../models/_lib/element-path';
import {pathsSameLevel} from './paths-same-level';

export function pathsCompared(pathA?: ElementPath, pathB?: ElementPath): number | null {
    if (!pathsSameLevel(pathA, pathB)) {
        return null;
    }

    const indexA = pathA![pathA!.length - 1];
    const indexB = pathB![pathB!.length - 1];
    const diff = indexA - indexB;

    if (diff === 0) {
        return 0;
    } else if (diff > 0) {
        return 1;
    } else {
        return -1;
    }
}
