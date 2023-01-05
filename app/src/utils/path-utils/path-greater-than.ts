import {ElementPath} from '../../models/_lib/element-path';
import {pathsCompared} from './paths-compared';

export function pathGreaterThan(pathA?: ElementPath, pathB?: ElementPath): boolean | null {
    const compared = pathsCompared(pathA, pathB);
    if (compared != null) {
        return compared > 0;
    }
    return null;
}