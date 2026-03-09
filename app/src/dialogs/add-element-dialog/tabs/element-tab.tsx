import React, {useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Button,
    Chip,
    Divider,
    Typography,
} from '@mui/material';
import ExpandMore from '@mui/icons-material/ExpandMore';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {SearchInput} from '../../../components/search-input/search-input';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {type BaseTabProps} from './base-tab-props';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementChildOptions} from '../../../data/element-type/element-child-options';
import {getElementIconForType} from '../../../data/element-type/element-icons';
import {
    elementTypeGroupLabels,
    ElementTypeGroups,
    getElementDescriptionForType,
    getElementGroupForType,
} from '../element-dialog-metadata';

const defaultExpandedGroups: Record<ElementTypeGroups, boolean> = {
    [ElementTypeGroups.Display]: true,
    [ElementTypeGroups.Information]: true,
    [ElementTypeGroups.Input]: true,
    [ElementTypeGroups.DateTime]: true,
    [ElementTypeGroups.Select]: true,
    [ElementTypeGroups.Group]: true,
    [ElementTypeGroups.Other]: true,
};

interface ElementOption {
    type: ElementType;
    group: ElementTypeGroups;
    name: string;
    description: string;
}

export function ElementTab({
    parentType,
    onAddElement,
    primaryActionLabel,
    primaryActionIcon,
    showElementInfo,
    highlightedElement,
    limitElementTypes,
}: BaseTabProps & {
    showElementInfo: (type: ElementType) => void;
    highlightedElement?: ElementType;
    limitElementTypes?: ElementType[];
}) {
    const [search, setSearch] = useState('');
    const [expandedGroups, setExpandedGroups] = useState<Record<ElementTypeGroups, boolean>>(defaultExpandedGroups);

    const options = useMemo<ElementOption[]>(() => {
        const childOptions = ElementChildOptions[parentType] ?? [];

        return childOptions
            .filter((type) => limitElementTypes == null || limitElementTypes.includes(type))
            .flatMap((type) => {
                const group = getElementGroupForType(type);
                if (group == null) {
                    return [];
                }

                return [{
                    type,
                    group,
                    name: getElementNameForType(type),
                    description: getElementDescriptionForType(type),
                }];
            });
    }, [limitElementTypes, parentType]);

    const filteredOptions = useMemo(() => {
        const trimmedSearch = search.trim();
        if (trimmedSearch.length === 0) {
            return [...options].sort((left, right) => left.name.localeCompare(right.name, 'de'));
        }

        const fuse = new Fuse(options, {
            threshold: 0.32,
            ignoreLocation: true,
            keys: [
                {name: 'name', weight: 0.6},
                {name: 'description', weight: 0.4},
            ],
        });

        return fuse.search(trimmedSearch).map((entry) => entry.item);
    }, [options, search]);

    const groupedOptions = useMemo(() => {
        const groups = new Map<ElementTypeGroups, ElementOption[]>();

        for (const option of filteredOptions) {
            const groupOptions = groups.get(option.group) ?? [];
            groupOptions.push(option);
            groups.set(option.group, groupOptions);
        }

        return groups;
    }, [filteredOptions]);

    const visibleGroupTypes = useMemo(() => (
        Object.values(ElementTypeGroups)
            .filter((group): group is ElementTypeGroups => typeof group === 'number')
            .filter((group) => (groupedOptions.get(group)?.length ?? 0) > 0)
    ), [groupedOptions]);

    const handleAddElement = (type: ElementType): void => {
        const newElement = generateElementWithDefaultValues(type);
        if (newElement != null) {
            onAddElement(newElement);
        }
    };

    return (
        <Box
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <Box
                sx={{
                    p: 2,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <SearchInput
                    label="Element suchen"
                    placeholder="Name oder Beschreibung durchsuchen"
                    value={search}
                    onChange={setSearch}
                    debounce={120}
                />
            </Box>

            <Box
                sx={{
                    flex: 1,
                    overflowY: 'auto',
                    pb: 1.5,
                }}
            >
                {
                    options.length === 0 &&
                    <Box sx={{mt: 2, px: 2}}>
                        <Alert severity="info">
                            Für diesen Bereich stehen keine passenden Formularelemente zur Verfügung.
                        </Alert>
                    </Box>
                }

                {
                    options.length > 0 && filteredOptions.length === 0 &&
                    <Box sx={{mt: 2, px: 2}}>
                        <Alert severity="info">
                            Es wurden keine Formularelemente gefunden, die zu Ihrer Suche passen.
                        </Alert>
                    </Box>
                }

                {
                    visibleGroupTypes.map((group, groupIndex) => (
                        <Accordion
                            key={group}
                            disableGutters
                            expanded={search.trim().length > 0 ? true : expandedGroups[group]}
                            onChange={(_, expanded) => {
                                if (search.trim().length > 0) {
                                    return;
                                }

                                setExpandedGroups((previous) => ({
                                    ...previous,
                                    [group]: expanded,
                                }));
                            }}
                            sx={{
                                boxShadow: 'none',
                                bgcolor: 'transparent',
                                '&::before': {
                                    display: 'none',
                                },
                                '&.Mui-expanded': {
                                    mt: 0,
                                    mb: 0,
                                },
                            }}
                        >
                            <AccordionSummary
                                expandIcon={search.trim().length === 0 ? <ExpandMore/> : undefined}
                                sx={{
                                    px: 2,
                                    minHeight: 56,
                                    bgcolor: 'rgba(15, 23, 42, 0.035)',
                                    borderTop: groupIndex === 0 ? 'none' : '1px solid',
                                    borderBottom: '1px solid',
                                    borderColor: 'divider',
                                    '& .MuiAccordionSummary-content': {
                                        my: 1.5,
                                    },
                                }}
                            >
                                <Box
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 1,
                                        minWidth: 0,
                                        pl: 1,
                                    }}
                                >
                                    <Typography fontWeight={700}>
                                        {elementTypeGroupLabels[group]}
                                    </Typography>
                                    <Chip
                                        size="small"
                                        label={`${groupedOptions.get(group)?.length ?? 0} ${(groupedOptions.get(group)?.length ?? 0) === 1 ? 'Element' : 'Elemente'}`}
                                    />
                                </Box>
                            </AccordionSummary>

                            <AccordionDetails sx={{p: 0}}>
                                {
                                    (groupedOptions.get(group) ?? []).map((option, index, groupOptions) => (
                                        <React.Fragment key={option.type}>
                                            <ElementRow
                                                option={option}
                                                isSelected={highlightedElement === option.type}
                                                primaryActionLabel={primaryActionLabel}
                                                primaryActionIcon={primaryActionIcon}
                                                onAdd={() => {
                                                    handleAddElement(option.type);
                                                }}
                                                onShowDetails={() => {
                                                    showElementInfo(option.type);
                                                }}
                                            />
                                            {
                                                index < groupOptions.length - 1 &&
                                                <Divider/>
                                            }
                                        </React.Fragment>
                                    ))
                                }
                            </AccordionDetails>
                        </Accordion>
                    ))
                }
            </Box>
        </Box>
    );
}

function ElementRow(props: {
    option: ElementOption;
    isSelected: boolean;
    primaryActionLabel: string;
    primaryActionIcon: React.ReactNode;
    onAdd: () => void;
    onShowDetails: () => void;
}) {
    const {
        option,
        isSelected,
        primaryActionLabel,
        primaryActionIcon,
        onAdd,
        onShowDetails,
    } = props;

    const Icon = getElementIconForType(option.type);

    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 1.75,
                px: 2.25,
                py: 1.9,
                bgcolor: isSelected ? 'action.hover' : 'transparent',
            }}
        >
            <Box
                sx={{
                    width: 38,
                    height: 38,
                    borderRadius: '50%',
                    bgcolor: 'grey.100',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                }}
            >
                <Icon sx={{fontSize: 20, color: 'text.secondary'}}/>
            </Box>

            <Box
                sx={{
                    minWidth: 0,
                    flex: 1,
                }}
            >
                <Typography
                    fontWeight={700}
                    sx={{
                        minWidth: 0,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {option.name}
                </Typography>

                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{mt: 0.5}}
                >
                    {option.description}
                </Typography>
            </Box>

            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.5,
                    flexShrink: 0,
                    pl: 1.5,
                }}
            >
                <Button
                    variant="text"
                    size="small"
                    startIcon={<InfoOutlinedIcon sx={{fontSize: 18}}/>}
                    onClick={onShowDetails}
                >
                    Details
                </Button>
                <Button
                    variant="contained"
                    size="small"
                    startIcon={primaryActionIcon}
                    onClick={onAdd}
                >
                    {primaryActionLabel}
                </Button>
            </Box>
        </Box>
    );
}
