import {type VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {type TeamEntity} from '../../../teams/entities/team-entity';
import {type User} from '../../../users/models/user';

export interface OrganigramDepartmentItem extends VDepartmentShadowedEntity {
    color: string;
    children: OrganigramDepartmentItem[];
    members: OrganigramUserItem[];
}

export interface OrganigramTeamItem extends TeamEntity {
    color: string;
    members: OrganigramUserItem[];
}

export type OrganigramUserItem = User;
