import {ElementType} from './element-type';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import ExpandOutlinedIcon from '@mui/icons-material/ExpandOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import NotesOutlinedIcon from '@mui/icons-material/NotesOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import TextFieldsOutlinedIcon from '@mui/icons-material/TextFieldsOutlined';
import NumbersOutlinedIcon from '@mui/icons-material/NumbersOutlined';
import TableChartOutlinedIcon from '@mui/icons-material/TableChartOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import PlaylistAddOutlinedIcon from '@mui/icons-material/PlaylistAddOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import CheckBoxOutlinedIcon from '@mui/icons-material/CheckBoxOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import ListAltOutlinedIcon from '@mui/icons-material/ListAltOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import ChecklistOutlinedIcon from '@mui/icons-material/ChecklistOutlined';
import TitleOutlinedIcon from '@mui/icons-material/TitleOutlined';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';

import {type SvgIcon} from '@mui/material';
import {type AnyElement} from '../../models/elements/any-element';
import {type SvgIconComponent} from '@mui/icons-material';

const ElementIcons: Record<ElementType, SvgIconComponent> = {
    [ElementType.Alert]: ErrorOutlineOutlinedIcon,
    [ElementType.Checkbox]: CheckBoxOutlinedIcon,
    [ElementType.Image]: ImageOutlinedIcon,
    [ElementType.Container]: MenuOutlinedIcon,
    [ElementType.Date]: CalendarMonthOutlinedIcon,
    [ElementType.Step]: InsertDriveFileOutlinedIcon,
    [ElementType.Root]: DescriptionOutlinedIcon,
    [ElementType.Headline]: TitleOutlinedIcon,
    [ElementType.MultiCheckbox]: ChecklistOutlinedIcon,
    [ElementType.Number]: NumbersOutlinedIcon,
    [ElementType.ReplicatingContainer]: PlaylistAddOutlinedIcon,
    [ElementType.Richtext]: NotesOutlinedIcon,
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
};

export function getElementIcon(element: AnyElement): typeof SvgIcon {
    if (element.type === ElementType.Container && element.storeLink != null) {
        return ExtensionOutlinedIcon;
    }

    return getElementIconForType(element.type);
}

export function getElementIconForType(elementType: ElementType): typeof SvgIcon {
    return ElementIcons[elementType];
}
