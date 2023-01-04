import {ElementPath} from '../../models/_lib/element-path';
import {pathsEqual} from './paths-equal';

export function pathsSameLevel(pathA?: ElementPath, pathB?: ElementPath) {
    return pathsEqual(pathA?.slice(0, -1), pathB?.slice(0, -1));
}
