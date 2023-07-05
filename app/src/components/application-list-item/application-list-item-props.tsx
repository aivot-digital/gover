import {ListApplication} from "../../models/entities/list-application";
import {DepartmentMembership} from "../../models/entities/department-membership";

export interface ApplicationListItemProps {
    application: ListApplication;
    onClone: (application: ListApplication) => void;
    onDelete: (application: ListApplication) => void;
    onNewVersion: (application: ListApplication) => void;
    memberships?: DepartmentMembership[];
}