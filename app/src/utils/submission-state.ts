import {OverridableComponent} from '@mui/material/OverridableComponent';
import {SvgIconTypeMap} from '@mui/material';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import CallReceivedOutlinedIcon from '@mui/icons-material/CallReceivedOutlined';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import {SubmissionStatus} from '../modules/submissions/enums/submission-status';
import AssignmentLateOutlinedIcon from '@mui/icons-material/AssignmentLateOutlined';
import EditNoteOutlinedIcon from '@mui/icons-material/EditNoteOutlined';
import ProductionQuantityLimitsOutlinedIcon from '@mui/icons-material/ProductionQuantityLimitsOutlined';
import SyncProblemOutlinedIcon from '@mui/icons-material/SyncProblemOutlined';
import {SubmissionListResponseDTO} from '../modules/submissions/dtos/submission-list-response-dto';

export interface SubmissionState {
    color: 'info' | 'error' | 'inherit' | 'success' | 'warning';
    label: string;
    icon: OverridableComponent<SvgIconTypeMap>;
}

export function createSubmissionState(submission: SubmissionListResponseDTO): SubmissionState {
    return {
        color: determineColor(submission),
        label: determineLabel(submission),
        icon: determineIcon(submission),
    };
}

export function determineLabel(sub: SubmissionListResponseDTO): string {
    switch (sub.status) {
        case SubmissionStatus.OpenForManualWork:
            return 'Bereit zur Bearbeitung';
        case SubmissionStatus.ManualWorkingOn:
            return 'In Bearbeitung';
        case SubmissionStatus.HasPaymentError:
            return 'Fehler bei Zahlung';
        case SubmissionStatus.HasDestinationError:
            return 'Übertragung an Schnittstelle fehlgeschlagen';
        case SubmissionStatus.Archived:
            return 'Abgeschlossen';
        case SubmissionStatus.Pending:
            return 'Verarbeitung durch System';
        default:
            return 'Unbekannt';
    }
}

function determineIcon(sub: SubmissionListResponseDTO): OverridableComponent<SvgIconTypeMap> {
    switch (sub.status) {
        case SubmissionStatus.OpenForManualWork:
            return AssignmentLateOutlinedIcon;
        case SubmissionStatus.ManualWorkingOn:
            return EditNoteOutlinedIcon;
        case SubmissionStatus.HasPaymentError:
            return ProductionQuantityLimitsOutlinedIcon;
        case SubmissionStatus.HasDestinationError:
            return SyncProblemOutlinedIcon;
        case SubmissionStatus.Archived:
            if (sub.destinationSuccess) {
                return CallReceivedOutlinedIcon;
            }
            return Inventory2OutlinedIcon;
        case SubmissionStatus.Pending:
            return HelpOutlineOutlinedIcon;
        default:
            return HelpOutlineOutlinedIcon;
    }
}

function determineColor(sub: SubmissionListResponseDTO): 'info' | 'error' | 'inherit' | 'success' | 'warning' {
    switch (sub.status) {
        case SubmissionStatus.OpenForManualWork:
            return 'info';
        case SubmissionStatus.ManualWorkingOn:
            return 'success';
        case SubmissionStatus.HasPaymentError:
        case SubmissionStatus.HasDestinationError:
            return 'error';
        case SubmissionStatus.Archived:
            return 'inherit';
        case SubmissionStatus.Pending:
            return 'warning';
        default:
            return 'warning';
    }
}