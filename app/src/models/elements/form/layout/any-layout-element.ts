import {type GroupLayout} from './group-layout';
import {type ReplicatingContainerLayout} from './replicating-container-layout';
import {type SummaryLayoutElement} from './summary-layout-element';
import {type ConfigLayoutElement} from './config-layout-element';

export type AnyLayoutElement =
    GroupLayout |
    ReplicatingContainerLayout |
    ConfigLayoutElement |
    SummaryLayoutElement;
