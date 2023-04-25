import styles from './element-tree-item-title.module.scss';
import {Box, IconButton, Tooltip, Typography} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {
    faBinaryCircleCheck,
    faChevronDown,
    faChevronRight,
    faCircleF,
    faDiamondExclamation,
    faListCheck,
    faMemoCircleCheck,
    faPlusCircle
} from '@fortawesome/pro-light-svg-icons';
import {ElementIcons} from '../../../../data/element-type/element-icons';
import {generateComponentTitle} from '../../../../utils/generate-component-title';
import {checkId} from '../../../../utils/id-utils';
import {hasElementFunction} from '../../../../utils/has-element-function';
import React from 'react';
import {ElementType} from '../../../../data/element-type/element-type';
import {ElementNames} from '../../../../data/element-type/element-names';
import {useTheme} from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import {hasUntestedChild} from '../../../../utils/has-untested-child';
import {RootElement} from '../../../../models/elements/root-element';
import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {
    selectUseIdsInComponentTree,
    selectUseTestMode,
    selectWarnDuplicateIds
} from '../../../../slices/admin-settings-slice';
import {ElementTreeItemTitleProps} from './element-tree-item-title-props';
import {AnyElement} from '../../../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../../../models/elements/any-element-with-children';
import {getStepIcon} from '../../../../data/step-icons';
import {selectLoadedApplication} from '../../../../slices/app-slice';
import {stringOrDefault} from "../../../../utils/string-utils";


export function ElementTreeItemTitle<T extends AnyElement>(props: ElementTreeItemTitleProps<T>) {
    const testMode = useAppSelector(selectUseTestMode);
    const useIdsInComponentTree = useAppSelector(selectUseIdsInComponentTree);
    const warnDuplicateIds = useAppSelector(selectWarnDuplicateIds);
    const root = useAppSelector(selectLoadedApplication)?.root;

    const handleSelect = () => {
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
        props.onSelect();
    };

    const addHighlightElement = () => {
        const elem = document.getElementById(props.element.id);
        if (elem) {
            elem.style.outline = '#86FFD388 solid 2px';
            elem.style.boxShadow = '0px 4px 20px rgba(179, 242, 219, 0.5)';
        }
    };

    const removeHighlightElement = () => {
        const elem = document.getElementById(props.element.id);
        if (elem) {
            elem.style.outline = 'none';
            elem.style.boxShadow = 'none';
        }
    };

    const handleToggleExpand = (event?: React.MouseEvent<HTMLElement>) => {
        if (props.onToggleExpanded) {
            props.onToggleExpanded();
        }
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        return false;
    };

    const preventBubble = (event?: React.MouseEvent<HTMLElement>) => {
        if (event) {
            event.stopPropagation();
            event.preventDefault();
        }
        return false;
    };

    const theme = useTheme();

    const statusIcons = determineIcons(testMode, warnDuplicateIds, root, props.element);
    const elementTitle = stringOrDefault(props.element.name, generateComponentTitle(props.element));
    const titleCharLimit = testMode ? 30 : 40;
    const elementIcon = props.element.type === ElementType.Step ? getStepIcon(props.element) : ElementIcons[props.element.type];

    return (
        <div
            className={styles.listItem}
            onDoubleClick={handleSelect}
            style={{display: 'flex'}}
            onMouseEnter={addHighlightElement}
            onMouseLeave={removeHighlightElement}
        >
            {
                props.onToggleExpanded ? (
                    <IconButton
                        onClick={handleToggleExpand}
                        onDoubleClick={preventBubble}
                        size={'small'}
                        sx={{mr: 1}}
                    >
                        <Typography
                            component="span"
                            sx={{
                                width: '18px',
                                height: '18px',
                                display: 'inline-block',
                                textAlign: 'center'
                            }}
                        >
                            <FontAwesomeIcon
                                icon={props.isExpanded ? faChevronDown : faChevronRight}
                                size={'1x'}
                            />
                        </Typography>
                    </IconButton>
                ) : <Box sx={{pl: 4.5}}/>
            }

            <Tooltip
                title={ElementNames[props.element.type]}
                arrow
            >
                <Box
                    className="element-tree-item-icon"
                    style={{
                        color: theme.palette.primary.dark,
                    }}
                >
                    <FontAwesomeIcon
                        icon={elementIcon}
                        fixedWidth
                        size={'lg'}
                    />
                </Box>
            </Tooltip>

            <Typography
                variant="body1"
                sx={{fontSize: '1rem', color: '#16191F'}}
            >
                {

                    useIdsInComponentTree ?
                        props.element.id :
                        (
                            ((elementTitle).length > titleCharLimit) ?
                                (<span
                                    title={elementTitle}
                                >{((elementTitle).substring(0, titleCharLimit - 1)) + '…'}</span>) :
                                elementTitle
                        )
                }
            </Typography>

            {
                (
                    props.element.type === ElementType.IntroductionStep ||
                    props.element.type === ElementType.Step ||
                    props.element.type === ElementType.SummaryStep ||
                    props.element.type === ElementType.SubmitStep
                ) &&
                <Chip
                    sx={{
                        ml: 1,
                        color: props.element.type === ElementType.Step ? theme.palette.primary.dark : '#bdbdbd',
                        borderColor: props.element.type === ElementType.Step ? theme.palette.primary.dark : '#bdbdbd'
                    }}
                    size="small"
                    label="Abschnitt"
                    variant="outlined"
                />
            }

            <div
                style={{marginLeft: 'auto'}}
            >

                {
                    statusIcons.map(icon => (
                        <Tooltip
                            title={icon.tooltip}
                            arrow
                            key={icon.tooltip}
                        >
                            <IconButton color={icon.color}>
                                <FontAwesomeIcon icon={icon.icon}/>
                            </IconButton>
                        </Tooltip>
                    ))
                }

                {
                    props.onShowAddDialog &&
                    <Tooltip
                        title="Element hinzufügen"
                        arrow
                    >
                        <IconButton onClick={props.onShowAddDialog}>
                            <FontAwesomeIcon icon={faPlusCircle}/>
                        </IconButton>
                    </Tooltip>
                }
            </div>
        </div>
    );
}

interface TreeElementIcon {
    color?: 'primary' | 'secondary' | 'error' | 'success' | 'warning';
    tooltip: string;
    icon: IconDefinition;
}

function determineIcons(useTestMode: boolean, warnDuplicateIds: boolean, root: RootElement | undefined, element: AnyElement): TreeElementIcon[] {
    const icons: TreeElementIcon[] = [];

    if (useTestMode) {
        const professionalTestMissing = element.testProtocolSet?.professionalTest == null;
        const technicalTestMissing = hasElementFunction(element) && element.testProtocolSet?.technicalTest == null;

        if (professionalTestMissing) {
            icons.push({
                color: 'warning',
                icon: faMemoCircleCheck,
                tooltip: 'Fachliche Prüfung ausstehend',
            });
        }

        if (technicalTestMissing) {
            icons.push({
                color: 'warning',
                icon: faBinaryCircleCheck,
                tooltip: 'Technische Prüfung ausstehend',
            });
        }

        if (!professionalTestMissing && !technicalTestMissing && isAnyElementWithChildren(element)) {
            if (hasUntestedChild(element)) {
                icons.push({
                    color: 'warning',
                    icon: faListCheck,
                    tooltip: 'Prüfung für Kind-Element ausstehend',
                });
            }
        }
    }

    if (warnDuplicateIds && root != null) {
        const msg = checkId(root, element.id);
        if (msg != null) {
            icons.push({
                color: 'error',
                icon: faDiamondExclamation,
                tooltip: msg,
            });
        }
    }

    if (hasElementFunction(element)) {
        if (element.testProtocolSet?.technicalTest != null) {
            icons.push({
                icon: faCircleF,
                tooltip: 'Individuelle Funktionen definiert',
            });
        } else {
            icons.push({
                color: 'error',
                icon: faCircleF,
                tooltip: 'Individuelle Funktionen erforderlich',
            });
        }
    }

    return icons;
}
