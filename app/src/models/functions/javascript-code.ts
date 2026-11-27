export interface JavascriptCode {
    code: string | null | undefined;
}

export function isJavascriptCode(value: any): value is JavascriptCode {
    return value != null && typeof value.code === 'string';
}