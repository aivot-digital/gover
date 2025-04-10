import {type DepartmentMembership} from '../../modules/departments/models/department-membership';
import {type User} from '../../models/entities/user';
import {FormListProjection} from '../../models/entities/form';

export interface ApplicationListItemProps {
    application: FormListProjection;
    onClone: (application: FormListProjection) => void;
    onDelete: (application: FormListProjection) => void;
    onNewVersion: (application: FormListProjection) => void;
    user: User;
    memberships: DepartmentMembership[];
}
