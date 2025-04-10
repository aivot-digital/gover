export interface JavascriptCode {
    code: string;
}

export function isJavascriptCode(value: any): value is JavascriptCode {
    return value != null && typeof value.code === 'string';
}