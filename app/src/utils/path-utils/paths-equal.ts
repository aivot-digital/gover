import {ElementPath} from '../../models/_lib/element-path';

export function pathsEqual(pathA?: ElementPath, pathB?: ElementPath) {
    return (
        pathA != null &&
        pathB != null &&
        pathA.length === pathB.length &&
        pathA.every((val, index) => val === pathB[index])
    );
}