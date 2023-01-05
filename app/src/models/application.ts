import {RootElement} from './elements/root-element';

export interface Application {
    id: number;
    slug: string;
    version: string;
    root: RootElement;
    code?: string;
    created: string;
    updated: string;
}
export function isApplication(obj: any): obj is Application {
    const assumedObject = obj as Application;
    return (
        assumedObject.id != null &&
        assumedObject.slug != null &&
        assumedObject.version != null &&
        assumedObject.root != null
    );
}
