import {ListApplicationGroup} from "../../models/lib/list-application-group";
import {ListApplication} from "../../models/entities/list-application";
import {DepartmentMembership} from "../../models/entities/department-membership";

export interface ApplicationListItemGroupProps {
    group: ListApplicationGroup;
    onClone: (application: ListApplication) => void;
    onDelete: (application: ListApplication) => void;
    onNewVersion: (application: ListApplication) => void;
    memberships?: DepartmentMembership[];
}