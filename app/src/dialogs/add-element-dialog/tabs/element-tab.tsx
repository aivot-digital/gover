import React, {useMemo, useState} from 'react';
import Fuse from 'fuse.js';
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Alert,
    Box,
    Chip,
    Divider,
    Typography,
} from '@mui/material';
import ExpandMore from '@mui/icons-material/ExpandMore';
import {SearchInput} from '../../../components/search-input/search-input';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {type BaseTabProps} from './base-tab-props';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementChildOptions, ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {getElementIconForType} from '../../../data/element-type/element-icons';
import {
    elementTypeGroupLabels,
    ElementTypeGroups,
    getElementDescriptionForType,
    getElementGroupForType,
} from '../element-dialog-metadata';
import {SelectionListRow} from '../../../components/selection-dialog/selection-list-row';
import {getSingleUseSectionAddDisabledReason} from '../../../data/element-type/single-use-section-types';

const defaultExpandedGroups: Record<ElementTypeGroups, boolean> = {
    [ElementTypeGroups.Display]: true,
    [ElementTypeGroups.Information]: true,
    [ElementTypeGroups.Input]: true,
    [ElementTypeGroups.DateTime]: true,
    [ElementTypeGroups.Select]: true,
    [ElementTypeGroups.Group]: true,
    [ElementTypeGroups.Step]: true,
    [ElementTypeGroups.Other]: true,
};

interface ElementOption {
    type: ElementType;
    group: ElementTypeGroups;
    name: string;
    description: string;
}

const elementSortOrdersByGroup: Partial<Record<ElementTypeGroups, Partial<Record<ElementType, number>>>> = {
    // Add another group here when its options need a semantic order instead of alphabetic sorting.
    // Example: [ElementTypeGroups.Information]: {[ElementType.Alert]: 0, [ElementType.RichText]: 1}
    [ElementTypeGroups.Step]: {
        [ElementType.Step]: 0,
        [ElementType.IntroductionStep]: 1,
        [ElementType.SummaryStep]: 2,
        [ElementType.SubmitStep]: 3,
    },
};

function compareElementOptions(left: ElementOption, right: ElementOption): number {
    if (left.group === right.group) {
        const sortOrder = elementSortOrdersByGroup[left.group];
        const leftOrder = sortOrder?.[left.type] ?? Number.MAX_SAFE_INTEGER;
        const rightOrder = sortOrder?.[right.type] ?? Number.MAX_SAFE_INTEGER;

        if (leftOrder !== rightOrder) {
            return leftOrder - rightOrder;
        }
    }

    return left.name.localeCompare(right.name, 'de');
}

export function ElementTab({
                               parentType,
                               parentElement,
                               allParents,
                               onAddElement,
                               primaryActionLabel,
                               primaryActionIcon,
                               showElementInfo,
                               highlightedElement,
                               limitElementTypes,
                               recentElementTypes = [],
                               displayContext,
                           }: BaseTabProps & {
    showElementInfo: (type: ElementType) => void;
    highlightedElement?: ElementType;
    limitElementTypes?: ElementType[];
    recentElementTypes?: ElementType[];
    displayContext: ElementDisplayContext;
}) {
    const [search, setSearch] = useState('');
    const [expandedGroups, setExpandedGroups] = useState<Record<ElementTypeGroups, boolean>>(defaultExpandedGroups);

    const options = useMemo<ElementOption[]>(() => {
        let childOptionSet: Set<ElementType> | null = null;

        if (allParents.length > 1) {
            for (const par of allParents.slice(1)) {
                const allowedChildOptionOfThisParent = ElementChildOptions[displayContext][par.type] ?? [];
                const allowedChildOptionOfThisParentSet = new Set(allowedChildOptionOfThisParent);

                if (childOptionSet == null) {
                    childOptionSet = allowedChildOptionOfThisParentSet;
                } else {
                    childOptionSet = childOptionSet.intersection(allowedChildOptionOfThisParentSet);
                }
            }
        } else {
            childOptionSet = new Set(ElementChildOptions[displayContext][parentType] ?? []);
        }

        const childOptions = childOptionSet != null ? Array.from(childOptionSet) : [];

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
    }, [allParents, displayContext, limitElementTypes, parentType]);

    const filteredOptions = useMemo(() => {
        const trimmedSearch = search.trim();
        if (trimmedSearch.length === 0) {
            return [...options].sort(compareElementOptions);
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

    const recentOptions = useMemo(() => {
        const optionsMap = new Map(options.map((option) => [option.type, option]));

        return recentElementTypes.flatMap((type) => {
            const option = optionsMap.get(type);
            return option == null ? [] : [option];
        });
    }, [options, recentElementTypes]);

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
        if (getSingleUseSectionAddDisabledReason(parentElement, type) != null) {
            return;
        }

        const newElement = generateElementWithDefaultValues(type, parentElement);
        if (newElement != null) {
            onAddElement(newElement);
        }
    };

    const renderOptionRows = (elementOptions: ElementOption[], showGroupLabel: boolean = false) => (
        elementOptions.map((option, index) => {
            const disabledReason = getSingleUseSectionAddDisabledReason(parentElement, option.type);

            return (
                <React.Fragment key={`${showGroupLabel ? 'recent' : 'group'}-${option.type}`}>
                    <ElementRow
                        option={option}
                        isSelected={highlightedElement === option.type}
                        primaryActionLabel={primaryActionLabel}
                        primaryActionIcon={primaryActionIcon}
                        titleAdornment={
                            showGroupLabel ?
                                <Chip size="small"
                                      label={elementTypeGroupLabels[option.group]}/> :
                                undefined
                        }
                        disabledReason={disabledReason}
                        onAdd={() => {
                            handleAddElement(option.type);
                        }}
                        onShowDetails={() => {
                            showElementInfo(option.type);
                        }}
                    />
                    {
                        index < elementOptions.length - 1 &&
                        <Divider/>
                    }
                </React.Fragment>
            );
        })
    );

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
                    search.trim().length === 0 && recentOptions.length > 0 &&
                    <Box
                        sx={{
                            borderBottom: '1px solid',
                            borderColor: 'divider',
                        }}
                    >
                        <Box
                            sx={{
                                px: 2,
                                py: 1.5,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                bgcolor: 'rgba(15, 23, 42, 0.035)',
                                borderBottom: '1px solid',
                                borderColor: 'divider',
                            }}
                        >
                            <Typography fontWeight={700}>
                                Zuletzt verwendet
                            </Typography>
                            <Chip
                                size="small"
                                label={`${recentOptions.length} ${recentOptions.length === 1 ? 'Element' : 'Elemente'}`}
                            />
                        </Box>
                        {renderOptionRows(recentOptions, true)}
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
                                {renderOptionRows(groupedOptions.get(group) ?? [])}
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
    titleAdornment?: React.ReactNode;
    disabledReason?: string;
    onAdd: () => void;
    onShowDetails: () => void;
}) {
    const {
        option,
        isSelected,
        primaryActionLabel,
        primaryActionIcon,
        titleAdornment,
        disabledReason,
        onAdd,
        onShowDetails,
    } = props;

    const Icon = getElementIconForType(option.type);

    return (
        <SelectionListRow
            icon={<Icon sx={{fontSize: 20, color: 'text.secondary'}}/>}
            title={option.name}
            titleAdornment={titleAdornment}
            description={option.description}
            selected={isSelected}
            primaryActionLabel={primaryActionLabel}
            primaryActionIcon={primaryActionIcon}
            primaryActionDisabled={disabledReason != null}
            primaryActionDisabledTooltip={disabledReason}
            onShowDetails={onShowDetails}
            onPrimaryAction={onAdd}
        />
    );
}
