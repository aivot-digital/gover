import { type ListApplication } from '../../models/entities/list-application';
import { type DepartmentMembership } from '../../models/entities/department-membership';
import { type User } from '../../models/entities/user';

export interface ApplicationListItemProps {
    application: ListApplication;
    onClone: (application: ListApplication) => void;
    onDelete: (application: ListApplication) => void;
    onNewVersion: (application: ListApplication) => void;
    user: User;
    memberships: DepartmentMembership[];
}
