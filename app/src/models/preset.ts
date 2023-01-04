import {ContainerElement} from './elements/form-elements/layout-elements/container-element';

export interface Preset {
    id: number;
    root: ContainerElement;
    created?: string;
    updated?: string;
}
