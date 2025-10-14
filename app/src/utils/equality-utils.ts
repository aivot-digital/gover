export function shallowEquals(obj1: any, obj2: any): boolean {
    if (obj1 === obj2) {
        return true;
    }

    if (obj1 == null || obj2 == null) {
        return false;
    }

    return (
        Object.keys(obj1).length === Object.keys(obj2).length &&
        Object.keys(obj1).every((key) => obj1[key] === obj2[key])
    );
}

export function deepEquals(obj1: any, obj2: any): boolean {
    if (obj1 === obj2) {
        return true;
    }

    if (obj1 == null || obj2 == null || typeof obj1 !== 'object' || typeof obj2 !== 'object') {
        return false;
    }

    const keys1 = Object.keys(obj1);
    const keys2 = Object.keys(obj2);

    if (keys1.length !== keys2.length) {
        console.log('length mismatch', keys1, keys2);
        return false;
    }

    for (const key of keys1) {
        if (!keys2.includes(key) || !deepEquals(obj1[key], obj2[key])) {
            console.log('key mismatch', key, obj1[key], obj2[key]);
            return false;
        }
    }

    return true;
}