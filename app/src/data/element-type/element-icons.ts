import {ElementType} from './element-type';
import MenuOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Menu';
import ExpandOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Expand';
import ImageOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Image';
import NotesOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Notes';
import ErrorOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';
import TextFieldsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/TextFields';
import NumbersOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Numbers';
import TableChartOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/TableChart';
import UploadFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/UploadFile';
import PlaylistAddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/PlaylistAdd';
import CalendarMonthOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CalendarMonth';
import AccessTimeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import CheckBoxOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckBox';
import CheckCircleOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import ListAltOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ListAlt';
import CheckCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import InsertDriveFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Draft';
import DescriptionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import ChecklistOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Checklist';
import TitleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Title';
import ExtensionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Extension';
import CalendarClock from '@aivot/mui-material-symbols-400-n25-outlined/CalendarClock';
import PlaceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LocationOn';
import LocalOfferOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Sell';
import GroupsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Groups';
import AssignmentIndOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AssignmentInd';
import FolderData from '@aivot/mui-material-symbols-400-n25-outlined/FolderData';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import CodeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import FunctionsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import AttachFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AttachFile';
import PaymentsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Payments';
import FolderOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Folder';
import LinkOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Link';

import {type AnyElement} from '../../models/elements/any-element';
import {type SvgIconComponent} from '../../types/svg-icon-component';
import Dashboard2 from '@aivot/mui-material-symbols-400-n25-outlined/Dashboard2';
import Segment from '@aivot/mui-material-symbols-400-n25-outlined/Segment';
import {getStepIcon} from '../step-icons';

const ElementIcons: Record<ElementType, SvgIconComponent> = {
    [ElementType.Alert]: ErrorOutlineOutlinedIcon,
    [ElementType.Checkbox]: CheckBoxOutlinedIcon,
    [ElementType.Image]: ImageOutlinedIcon,
    [ElementType.GroupLayout]: MenuOutlinedIcon,
    [ElementType.Date]: CalendarMonthOutlinedIcon,
    [ElementType.Step]: InsertDriveFileOutlinedIcon,
    [ElementType.FormLayout]: DescriptionOutlinedIcon,
    [ElementType.Headline]: TitleOutlinedIcon,
    [ElementType.MultiCheckbox]: ChecklistOutlinedIcon,
    [ElementType.Number]: NumbersOutlinedIcon,
    [ElementType.ReplicatingContainer]: PlaylistAddOutlinedIcon,
    [ElementType.RichText]: NotesOutlinedIcon,
    [ElementType.Radio]: CheckCircleOutlineOutlinedIcon,
    [ElementType.Select]: ListAltOutlinedIcon,
    [ElementType.Spacer]: ExpandOutlinedIcon,
    [ElementType.Table]: TableChartOutlinedIcon,
    [ElementType.Text]: TextFieldsOutlinedIcon,
    [ElementType.Time]: AccessTimeOutlinedIcon,
    [ElementType.IntroductionStep]: InfoOutlinedIcon,
    [ElementType.SummaryStep]: ErrorOutlineOutlinedIcon,
    [ElementType.SubmitStep]: CheckCircleOutlineOutlinedIcon,
    [ElementType.SubmittedStep]: CheckCircleOutlinedIcon,
    [ElementType.FileUpload]: UploadFileOutlinedIcon,
    [ElementType.DialogLayout]: UploadFileOutlinedIcon,
    [ElementType.StepperLayout]: UploadFileOutlinedIcon,
    [ElementType.ConfigLayout]: UploadFileOutlinedIcon,
    [ElementType.FunctionInput]: UploadFileOutlinedIcon,
    [ElementType.CodeInput]: CodeOutlinedIcon,
    [ElementType.RichTextInput]: UploadFileOutlinedIcon,
    [ElementType.UiDefinitionInput]: Dashboard2,
    [ElementType.IdentityConfigElement]: UploadFileOutlinedIcon,
    [ElementType.TabLayout]: UploadFileOutlinedIcon,
    [ElementType.ChipInput]: LocalOfferOutlinedIcon,
    [ElementType.DateTime]: CalendarClock,
    [ElementType.DateRange]: CalendarMonthOutlinedIcon,
    [ElementType.TimeRange]: AccessTimeOutlinedIcon,
    [ElementType.DateTimeRange]: CalendarClock,
    [ElementType.MapPoint]: PlaceOutlinedIcon,
    [ElementType.DomainAndUserSelect]: GroupsOutlinedIcon,
    [ElementType.AssignmentContext]: AssignmentIndOutlinedIcon,
    [ElementType.DataModelSelect]: FolderData,
    [ElementType.DataObjectSelect]: DataObject,
    [ElementType.NoCodeInput]: FunctionsOutlinedIcon,
    [ElementType.SummaryLayout]: Segment,
    [ElementType.ProcessDataKeyInput]: DataObject,
    [ElementType.ProcessInstanceAttachmentSetSelect]: AttachFileOutlinedIcon,
    [ElementType.ProcessIdentityIdInput]: LocalOfferOutlinedIcon,
    [ElementType.ProcessIdentitySelect]: LocalOfferOutlinedIcon,
    [ElementType.HtmlTemplateInput]: DescriptionOutlinedIcon,
    [ElementType.StoragePathSelector]: FolderOutlinedIcon,
    [ElementType.ProcessAttachmentDisplay]: AttachFileOutlinedIcon,
    [ElementType.PaymentConfigElement]: PaymentsOutlinedIcon,
    [ElementType.LinkButton]: LinkOutlinedIcon,
};

export function getElementIcon(element: AnyElement): SvgIconComponent {
    if (element.type === ElementType.GroupLayout && element.marketplaceLink != null) {
        return ExtensionOutlinedIcon;
    }

    switch (element.type) {
        case ElementType.Step:
        case ElementType.IntroductionStep:
        case ElementType.SummaryStep:
        case ElementType.SubmitStep:
        case ElementType.SubmittedStep:
            return getStepIcon(element);
    }

    return getElementIconForType(element.type);
}

export function getElementIconForType(elementType: ElementType): SvgIconComponent {
    return ElementIcons[elementType];
}
