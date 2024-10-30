import React from 'react';
import {IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemText, ListSubheader, Tooltip} from '@mui/material';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {type BaseTabProps} from './base-tab-props';
import {type ElementTypesMap} from '../../../data/element-type/element-types-map';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementChildOptions} from '../../../data/element-type/element-child-options';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {getElementIconForType} from '../../../data/element-type/element-icons';

enum ElementTypeGroups {
    Display,
    Information,
    Input,
    DateTime,
    Select,
    Group,
    Other
}

const elementTypeGroupsLabels: { [key in ElementTypeGroups]: string } = {
    [ElementTypeGroups.Display]: 'Darstellung',
    [ElementTypeGroups.Information]: 'Information',
    [ElementTypeGroups.Select]: 'Auswahl',
    [ElementTypeGroups.Input]: 'Eingabe',
    [ElementTypeGroups.DateTime]: 'Datum und Zeit',
    [ElementTypeGroups.Group]: 'Gruppierung',
    [ElementTypeGroups.Other]: 'Sonstige',
};

const elementGroupMap: ElementTypesMap<ElementTypeGroups | null> = {
    [ElementType.Alert]: ElementTypeGroups.Information,
    [ElementType.Image]: ElementTypeGroups.Information,
    [ElementType.Container]: ElementTypeGroups.Display,
    [ElementType.Step]: null,
    [ElementType.Root]: null,
    [ElementType.Checkbox]: ElementTypeGroups.Select,
    [ElementType.Date]: ElementTypeGroups.DateTime,
    [ElementType.Headline]: ElementTypeGroups.Information,
    [ElementType.MultiCheckbox]: ElementTypeGroups.Select,
    [ElementType.Number]: ElementTypeGroups.Input,
    [ElementType.ReplicatingContainer]: ElementTypeGroups.Input,
    [ElementType.Richtext]: ElementTypeGroups.Information,
    [ElementType.Radio]: ElementTypeGroups.Select,
    [ElementType.Select]: ElementTypeGroups.Select,
    [ElementType.Spacer]: ElementTypeGroups.Display,
    [ElementType.Table]: ElementTypeGroups.Input,
    [ElementType.Text]: ElementTypeGroups.Input,
    [ElementType.Time]: ElementTypeGroups.DateTime,
    [ElementType.FileUpload]: ElementTypeGroups.Input,

    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
};

export function ElementTab({
    parentType,
    onAddElement,
    showElementInfo,
    highlightedElement,
}: BaseTabProps & {
    showElementInfo: (type: ElementType) => void;
    highlightedElement?: ElementType;
}): JSX.Element {
    const childOptions = ElementChildOptions[parentType] ?? [];

    const optionGroups = childOptions.reduce<{ [key in ElementTypeGroups]?: ElementType[] }>((groups, child) => {
        const childGroup = elementGroupMap[child] ?? ElementTypeGroups.Other;
        const groupChildren = groups[childGroup] ?? [];
        groupChildren.push(child);
        groups[childGroup] = groupChildren;
        return groups;
    }, {});

    return (
        <List
            dense
        >
            {
                Object.keys(ElementTypeGroups).map((groupString, index) => (
                    (optionGroups[parseInt(groupString) as ElementTypeGroups] ?? []).length > 0 &&
                    <React.Fragment key={index}>
                        <ListSubheader
                            component="div"
                            id={`element-list-subheader-${index}`}
                            sx={{
                                pl: '26px',
                                lineHeight: '30px',
                                mt: 1,
                                textTransform: 'uppercase',
                            }}
                        >
                            {elementTypeGroupsLabels[parseInt(groupString) as ElementTypeGroups]}
                        </ListSubheader>

                        {
                            (optionGroups[parseInt(groupString) as ElementTypeGroups] ?? []).map((type) => {
                                const Icon = getElementIconForType(type);
                                return (
                                    <ListItem
                                        key={type}
                                        disablePadding
                                        secondaryAction={
                                            <Tooltip title="Mehr Informationen">
                                                <IconButton onClick={() => {showElementInfo(type);}}>
                                                    <InfoOutlinedIcon/>
                                                </IconButton>
                                            </Tooltip>
                                        }

                                    >
                                        <ListItemButton
                                            onClick={() => {
                                                const newElement = generateElementWithDefaultValues(type);
                                                if (newElement != null) {
                                                    onAddElement(newElement);
                                                }
                                            }}
                                            selected={highlightedElement === type}
                                        >
                                            <ListItemIcon sx={{pl: 1.5}}>
                                                <Icon/>
                                            </ListItemIcon>
                                            <ListItemText
                                                disableTypography
                                                primary={getElementNameForType(type)}
                                            />
                                        </ListItemButton>
                                    </ListItem>
                                );
                            })
                        }
                    </React.Fragment>
                ))
            }
        </List>
    );
}
