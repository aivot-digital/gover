type CamelCase<S extends string> = S extends `${infer P1}_${infer P2}${infer P3}`
    ? `${Lowercase<P1>}${Uppercase<P2>}${CamelCase<P3>}`
    : Lowercase<S>

type KeysToCamelCase<T> = {
    [K in keyof T as CamelCase<string & K>]: T[K]
}


type CamelToSnakeCase<S extends string> = S extends `${infer T}${infer U}` ?
    `${T extends Capitalize<T> ? "_" : ""}${Lowercase<T>}${CamelToSnakeCase<U>}` :
    S

export type KeysToSnakeCase<T> = {
    [K in keyof T as CamelToSnakeCase<string & K>]: T[K]
}

export function snakeToCamel<T>(obj: KeysToSnakeCase<T>): T {
    const result: any = {};
    for (const key in obj) {
        const camelKey = key.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
        result[camelKey] = obj[key];
    }
    console.log(result);
    return result as T;
}