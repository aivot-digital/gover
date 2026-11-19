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

/** Returns the comparison form of a value that represents one domain choice, or undefined for ordinary objects. */
export type AtomicComparisonValue = (value: unknown) => object | undefined;

export function deepEquals(obj1: any, obj2: any, atomicValue?: AtomicComparisonValue): boolean {
    if (obj1 === obj2) {
        return true;
    }

    if (obj1 == null || obj2 == null || typeof obj1 !== 'object' || typeof obj2 !== 'object') {
        return false;
    }

    if (atomicValue != null) {
        const left = atomicValue(obj1);
        const right = atomicValue(obj2);
        if (left !== undefined || right !== undefined) {
            return left !== undefined && right !== undefined && deepEquals(left, right);
        }
    }

    const keys1 = Object.keys(obj1);
    const keys2 = Object.keys(obj2);

    if (keys1.length !== keys2.length) {
        return false;
    }

    for (const key of keys1) {
        if (!keys2.includes(key) || !deepEquals(obj1[key], obj2[key], atomicValue)) {
            return false;
        }
    }

    return true;
}
