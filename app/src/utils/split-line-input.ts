import React from 'react';
import {stringOrDefault} from "./string-utils";

export function splitLineInput(input?: string): string[] {
    return stringOrDefault(input, '').split('\n');
}

export function splitLineInputEvent(event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): string[] {
    return splitLineInput(event.target.value);
}

export function normalizeLines(lines?: string[]): string[] {
    return (lines ?? []).map(line => line.trim()).filter(line => line.length > 0);
}
