import {type VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {type TeamEntity} from '../../../teams/entities/team-entity';
import {type User} from '../../../users/models/user';

export interface OrganizationChartDepartmentItem extends VDepartmentShadowedEntity {
    color: string;
    children: OrganizationChartDepartmentItem[];
    members: OrganizationChartUserItem[];
}

export interface OrganizationChartTeamItem extends TeamEntity {
    color: string;
    members: OrganizationChartUserItem[];
}

export type OrganizationChartUserItem = User;
