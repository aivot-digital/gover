import styles from './element-tree-item-title.module.scss';
import {Box, IconButton, type SvgIcon, Tooltip, Typography} from '@mui/material';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {checkId} from '../../utils/id-utils';
import {getFunctionStatus} from '../../utils/function-status-utils';
import React from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {getElementName} from '../../data/element-type/element-names';
import {useTheme} from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import {hasUntestedChild} from '../../utils/has-untested-child';
import {type RootElement} from '../../models/elements/root-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectTreeElementSearch, selectUseIdsInComponentTree, selectUseTestMode, selectWarnDuplicateIds} from '../../slices/admin-settings-slice';
import {type ElementTreeItemTitleProps} from './element-tree-item-title-props';
import {type AnyElement} from '../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {getStepIcon} from '../../data/step-icons';
import {selectLoadedForm} from '../../slices/app-slice';
import {findNoCodeUsage} from '../../utils/find-no-code-usage';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import KeyboardArrowDownOutlinedIcon from '@mui/icons-material/KeyboardArrowDownOutlined';
import KeyboardArrowRightOutlinedIcon from '@mui/icons-material/KeyboardArrowRightOutlined';
import GradingOutlinedIcon from '@mui/icons-material/GradingOutlined';
import InventoryOutlinedIcon from '@mui/icons-material/InventoryOutlined';
import ChecklistOutlinedIcon from '@mui/icons-material/ChecklistOutlined';
import ReportOutlinedIcon from '@mui/icons-material/ReportOutlined';
import OfflineBoltOutlinedIcon from '@mui/icons-material/OfflineBoltOutlined';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import {getElementIcon} from '../../data/element-type/element-icons';

const highlightOutlineStyle = '#86FFD388 solid 2px';
const highlightBoxShadowStyle = '0px 4px 20px rgba(179, 242, 219, 0.5)';

export function ElementTreeItemTitle<T extends AnyElement>(props: ElementTreeItemTitleProps<T>): JSX.Element {
    const testMode = useAppSelector(selectUseTestMode);
    const useIdsInComponentTree = useAppSelector(selectUseIdsInComponentTree);
    const warnDuplicateIds = useAppSelector(selectWarnDuplicateIds);
    const root = useAppSelector(selectLoadedForm)?.root;
    const treeElementSearch = useAppSelector(selectTreeElementSearch);

    const handleSelect = (): void => {
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
        props.onSelect();
    };

    const addHighlightElement = (): void => {
        const elem = document.getElementById(props.element.id);
        if (elem != null) {
            elem.style.outline = highlightOutlineStyle;
            elem.style.boxShadow = highlightBoxShadowStyle;
        }
    };

    const removeHighlightElement = (): void => {
        const elem = document.getElementById(props.element.id);
        if (elem != null) {
            elem.style.outline = 'none';
            elem.style.boxShadow = 'none';
        }
    };

    const handleToggleExpand = (event?: React.MouseEvent<HTMLElement>): boolean => {
        if (props.onToggleExpanded != null) {
            props.onToggleExpanded();
        }
        if (event != null) {
            event.stopPropagation();
            event.preventDefault();
        }
        return false;
    };

    const preventBubble = (event?: React.MouseEvent<HTMLElement>): boolean => {
        if (event != null) {
            event.stopPropagation();
            event.preventDefault();
        }
        return false;
    };

    const theme = useTheme();

    const statusIcons = determineIcons(testMode, warnDuplicateIds, root, props.element);
    const elementTitle = generateComponentTitle(props.element);
    const titleCharLimit = testMode ? 30 : 40;
    const ElementIcon = props.element.type === ElementType.Step ? getStepIcon(props.element) : getElementIcon(props.element);
    const matchesSearch = treeElementSearch != null && treeElementSearch.length > 2 && (elementTitle.toLowerCase().includes(treeElementSearch.toLowerCase()) || props.element.id.toLowerCase().includes(treeElementSearch.toLocaleLowerCase()));

    return (
        <div
            className={styles.listItem}
            onDoubleClick={handleSelect}
            style={{
                display: 'flex',
                outline: matchesSearch ? highlightOutlineStyle : undefined,
                boxShadow: matchesSearch ? highlightBoxShadowStyle : undefined,
            }}
            onMouseEnter={addHighlightElement}
            onMouseLeave={removeHighlightElement}
        >
            {
                (props.onToggleExpanded != null) ?
                    (
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
                                    textAlign: 'center',
                                }}
                            >
                                {
                                    props.isExpanded === true ?
                                        (
                                            <KeyboardArrowDownOutlinedIcon
                                                sx={{transform: 'translate(-0.125rem, -0.125rem)'}}
                                            />
                                        ) :
                                        (
                                            <KeyboardArrowRightOutlinedIcon
                                                sx={{transform: 'translate(-0.125rem, -0.125rem)'}}
                                            />
                                        )
                                }
                            </Typography>
                        </IconButton>
                    ) :
                    <Box sx={{pl: 4.5}}/>
            }

            <Tooltip
                title={getElementName(props.element)}
                arrow
            >
                <Box
                    className="element-tree-item-icon"
                    style={{
                        color: theme.palette.primary.dark,
                    }}
                >
                    <ElementIcon
                        sx={{
                            fontSize: '1.75rem',
                            transform: 'translateY(2px)',
                            mr: 0.5,
                        }}
                    />
                </Box>
            </Tooltip>

            <Typography
                variant="body1"
                sx={{
                    fontSize: '1rem',
                    color: '#16191F',
                }}
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
                        borderColor: props.element.type === ElementType.Step ? theme.palette.primary.dark : '#bdbdbd',
                    }}
                    size="small"
                    label="Abschnitt"
                    variant="outlined"
                />
            }

            <div
                style={{
                    marginLeft: 'auto',
                }}
            >
                {
                    statusIcons.map((icon) => {
                        const Icon = icon.icon;
                        return (
                            <Tooltip
                                title={icon.tooltip}
                                arrow
                                key={icon.tooltip}
                                sx={{
                                    cursor: 'help'
                                }}
                            >
                                <IconButton color={icon.color} disableRipple>
                                    <Icon/>
                                </IconButton>
                            </Tooltip>
                        );
                    })
                }

                {
                    props.editable &&
                    (props.onShowAddDialog != null) &&
                    <Tooltip
                        title="Element hinzufügen"
                        arrow
                    >
                        <IconButton onClick={props.onShowAddDialog}>
                            <AddCircleOutlineOutlinedIcon/>
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
    icon: typeof SvgIcon;
}

function determineIcons(useTestMode: boolean, warnDuplicateIds: boolean, root: RootElement | undefined, element: AnyElement): TreeElementIcon[] {
    const icons: TreeElementIcon[] = [];
    const functionStatus = getFunctionStatus(element);

    if (useTestMode) {
        const professionalTestMissing = element.testProtocolSet?.professionalTest == null;
        const technicalTestMissing = functionStatus.length > 0 && element.testProtocolSet?.technicalTest == null;

        if (professionalTestMissing) {
            icons.push({
                color: 'warning',
                icon: GradingOutlinedIcon,
                tooltip: 'Fachliche Prüfung ausstehend',
            });
        }

        if (technicalTestMissing) {
            icons.push({
                color: 'warning',
                icon: InventoryOutlinedIcon,
                tooltip: 'Technische Prüfung ausstehend',
            });
        }

        if (!professionalTestMissing && !technicalTestMissing && isAnyElementWithChildren(element)) {
            if (hasUntestedChild(element)) {
                icons.push({
                    color: 'warning',
                    icon: ChecklistOutlinedIcon,
                    tooltip: 'Prüfung für Kind-Element ausstehend',
                });
            }
        }
    }

    if ("technical" in element && element.technical) {
        icons.push({
            icon: VisibilityOffOutlinedIcon,
            tooltip: 'Technisches Feld (im Formular nicht sichtbar)',
        });
    }

    if (warnDuplicateIds && root != null) {
        const msg = checkId(root, element.id);
        if (msg != null) {
            icons.push({
                color: 'error',
                icon: ReportOutlinedIcon,
                tooltip: msg,
            });
        }
    }

    const noCodeUsages = root != null ? findNoCodeUsage(element, root) : [];
    if (noCodeUsages.length > 0) {
        icons.push({
            icon: OfflineBoltOutlinedIcon,
            tooltip: 'Von No-Code Funktion referenziert: ' + noCodeUsages.map(generateComponentTitle).join(', '),
        });
    }

    if (functionStatus.length > 0) {
        if (functionStatus.every((s) => s.status === 'done')) {
            icons.push({
                icon: BuildCircleOutlinedIcon,
                tooltip: 'Individuelle Funktionen definiert',
            });
        } else if (functionStatus.some((s) => s.status === 'todo')) {
            icons.push({
                color: 'error',
                icon: BuildCircleOutlinedIcon,
                tooltip: 'Individuelle Funktionen erforderlich',
            });
        } else if (functionStatus.some((s) => s.status === 'unnecessary')) {
            icons.push({
                color: 'error',
                icon: HelpOutlineOutlinedIcon,
                tooltip: 'Individuelle Funktionen definiert aber keine Anforderung vorhanden',
            });
        }
    }

    if (element.metadata && Object.values(element.metadata).length !== 0) {
        if((element.metadata?.bundIdMapping || element.metadata?.bayernIdMapping || element.metadata?.shIdMapping || element.metadata?.mukMapping)){
            icons.push({
                icon: AccountCircleOutlinedIcon,
                tooltip: 'Verknüpfung mit Nutzerkonto vorhanden',
            });
        }
    }

    return icons;
}
