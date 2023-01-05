import {ContainerElement} from './container-element';
import {ReplicatingContainerElement} from './replicating-container-element';

export type AnyLayoutElement = (
    ContainerElement |
    ReplicatingContainerElement
    );
