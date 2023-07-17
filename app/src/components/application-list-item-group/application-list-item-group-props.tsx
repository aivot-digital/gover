import {type ListApplicationGroup} from '../../models/lib/list-application-group';
import {type ListApplication} from '../../models/entities/list-application';
import {type DepartmentMembership} from '../../models/entities/department-membership';
import {type User} from '../../models/entities/user';

export interface ApplicationListItemGroupProps {
    group: ListApplicationGroup;
    onClone: (application: ListApplication) => void;
    onDelete: (application: ListApplication) => void;
    onNewVersion: (application: ListApplication) => void;
    memberships: DepartmentMembership[];
    user: User;
}
