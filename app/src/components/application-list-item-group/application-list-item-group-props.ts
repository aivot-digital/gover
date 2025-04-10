import {type ListApplicationGroup} from '../../models/lib/list-application-group';
import {type DepartmentMembership} from '../../modules/departments/models/department-membership';
import {type User} from '../../models/entities/user';
import {FormListProjection} from '../../models/entities/form';

export interface ApplicationListItemGroupProps {
    group: ListApplicationGroup;
    onClone: (application: FormListProjection) => void;
    onDelete: (application: FormListProjection) => void;
    onNewVersion: (application: FormListProjection) => void;
    memberships: DepartmentMembership[];
    user: User;
}
