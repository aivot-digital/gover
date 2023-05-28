import {
    Alert,
    AlertTitle,
    Box,
    Dialog,
    DialogContent,
    FormControlLabel,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    ListSubheader,
    Switch,
    Tab,
    Tabs,
} from '@mui/material';
import {AddElementDialogProps} from './add-element-dialog-props';
import React, {useEffect, useState} from 'react';
import {ElementChildOptions} from '../../data/element-type/element-child-options';
import {ElementIcons} from '../../data/element-type/element-icons';
import {ElementNames} from '../../data/element-type/element-names';
import {Preset} from '../../models/entities/preset';
import {PresetsService} from '../../services/presets.service';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome'
import {ElementType} from '../../data/element-type/element-type';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {faLayerPlus} from '@fortawesome/pro-light-svg-icons';
import {ElementTypesMap} from '../../data/element-type/element-types-map';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {cloneElement} from "../../utils/clone-element";

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
}

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
}

export function AddElementDialog({parentType, onAddElement, onClose}: AddElementDialogProps) {
    const [presets, setPresets] = useState<Preset[]>([]);
    const [currentTab, setCurrentTab] = useState(0);

    useEffect(() => {
        PresetsService.list()
            .then(response => {
                setPresets(response._embedded.presets);
            });
    }, [parentType, setPresets]);

    const addPresetElement = (preset: Preset) => {
        onAddElement(cloneElement(preset.root, true));
    };

    const childOptions = ElementChildOptions[parentType] ?? [];

    const optionGroups = childOptions.reduce((groups, child) => {
        let childGroup = elementGroupMap[child] ?? ElementTypeGroups.Other;
        const groupChildren = groups[childGroup] ?? [];
        groupChildren.push(child);
        groups[childGroup] = groupChildren;
        return groups;
    }, {} as { [key in ElementTypeGroups]?: ElementType[] });

    return (
        <Dialog
            open={true}
            onClose={onClose}
            fullWidth
        >
            <DialogTitleWithClose
                id={'add-component-dialog-title'}
                onClose={onClose}
                closeTooltip="Schließen"
            >
                {parentType === ElementType.Root ? 'Neuen Abschnitt hinzufügen' : 'Neues Element hinzufügen'}
            </DialogTitleWithClose>

            <Tabs
                value={currentTab}
                onChange={(evt, val) => setCurrentTab(val)}
                sx={{borderBottom: '1px solid #E0E0E0', mt: -1}}
            >
                <Tab
                    label={parentType === ElementType.Root ? 'Basis-Abschnitte' : 'Basis-Elemente'}
                    value={0}
                />
                <Tab
                    label="Vorlagen"
                    value={1}
                />
            </Tabs>

            {
                currentTab === 0 &&
                <List
                    dense
                >
                    {
                        Object.keys(ElementTypeGroups).map((groupString, index) => (
                            (optionGroups[parseInt(groupString) as ElementTypeGroups] ?? []).length > 0 &&
                            <React.Fragment key={index}>
                                <ListSubheader
                                    component="div"
                                    id={'element-list-subheader-' + index}
                                    sx={{pl: '26px', lineHeight: '30px', mt: 1, textTransform: 'uppercase'}}
                                >
                                    {elementTypeGroupsLabels[parseInt(groupString) as ElementTypeGroups]}
                                </ListSubheader>

                                {
                                    (optionGroups[parseInt(groupString) as ElementTypeGroups] ?? []).map(type => (
                                        <ListItem
                                            key={type}
                                            disablePadding
                                        >
                                            <ListItemButton
                                                onClick={() => {
                                                    const newElement = generateElementWithDefaultValues(type);
                                                    if (newElement != null) {
                                                        onAddElement(newElement);
                                                    }
                                                }}
                                            >
                                                <ListItemIcon sx={{pl: 1.5}}>
                                                    <FontAwesomeIcon icon={ElementIcons[type]}/>
                                                </ListItemIcon>
                                                <ListItemText
                                                    disableTypography
                                                    primary={ElementNames[type]}
                                                />
                                            </ListItemButton>
                                        </ListItem>
                                    ))
                                }
                            </React.Fragment>
                        ))
                    }
                </List>
            }
            {
                currentTab === 1 &&
                <div>
                    {
                        (
                            presets.length > 0 ? (
                                <List
                                    dense
                                >
                                    {
                                        presets
                                            .map(preset => (
                                                <ListItem
                                                    key={preset.root.name}
                                                    disablePadding
                                                >
                                                    <ListItemButton onClick={() => addPresetElement(preset)}>
                                                        <ListItemIcon sx={{pl: 1.5}}>
                                                            <FontAwesomeIcon icon={ElementIcons[ElementType.Container]}/>
                                                        </ListItemIcon>
                                                        <ListItemText
                                                            disableTypography
                                                            primary={preset.root.name}
                                                        />
                                                    </ListItemButton>
                                                </ListItem>
                                            ))
                                    }
                                </List>
                            ) : (
                                <DialogContent>
                                    <Alert
                                        severity="info"
                                        variant={'outlined'}
                                        iconMapping={{
                                            info: <FontAwesomeIcon icon={faLayerPlus}/>,
                                        }}
                                    >
                                        <AlertTitle>Es existieren noch keine Vorlagen</AlertTitle>
                                        Sie können neue Vorlagen erstellen, in dem Sie bestehende Elemente oder
                                        Abschnitte im
                                        Bearbeitungs-Modus durch einen Klick auf die Schaltfläche "Als Vorlage
                                        speichern" am
                                        unteren Bildschirmrand hinzufügen.
                                    </Alert>
                                </DialogContent>
                            )
                        )
                    }
                </div>
            }
        </Dialog>
    );
}
