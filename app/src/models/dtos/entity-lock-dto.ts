import {EntityLockState} from '../../data/entity-lock-state';

export interface EntityLockDto {
    state: EntityLockState;
    lockedBy?: string;
}