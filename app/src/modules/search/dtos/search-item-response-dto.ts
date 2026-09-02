import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';

export interface SearchItemResponseDto {
    id: string;
    label: string;
    originTable: ServerEntityType;
}