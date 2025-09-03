import {type DepartmentMembership} from '../../modules/departments/models/department-membership';
import {type User} from '../../models/entities/user';
import {FormListProjection} from '../../models/entities/form';
import {FormListResponseDTO} from '../../modules/forms/dtos/form-list-response-dto';
import {FormCitizenListResponseDTO} from '../../modules/forms/dtos/form-citizen-list-response-dto';

export interface ApplicationListItemProps {
    form: FormListResponseDTO | FormCitizenListResponseDTO;
    onClone: (form: FormListResponseDTO) => void;
    onDelete: (form: FormListResponseDTO) => void;
    onNewVersion: (form: FormListResponseDTO) => void;
    user: User;
    memberships: DepartmentMembership[];
}
