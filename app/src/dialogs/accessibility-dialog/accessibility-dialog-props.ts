import {DialogProps} from '@mui/material/Dialog';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';

export interface AccessibilityDialogProps extends DialogProps {
    form?: FormLayoutElement;
    version?: Pick<ProcessVersionEntity, 'accessibilityDepartmentId' | 'processSpecificAccessibilityStatement'>;
    departmentId?: number | null;
    onHide: () => void;
    isListingPage?: boolean;
}
