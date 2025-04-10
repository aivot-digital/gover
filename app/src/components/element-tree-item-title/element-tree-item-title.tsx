import styles from './element-tree-item-title.module.scss';
import {Box, IconButton, Tooltip, Typography} from '@mui/material';
import {generateComponentTitle} from '../../utils/generate-component-title';
import React from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {getElementName} from '../../data/element-type/element-names';
import {useTheme} from '@mui/material/styles';
import Chip from '@mui/material/Chip';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectTreeElementSearch, selectUseIdsInComponentTree, selectUseTestMode} from '../../slices/admin-settings-slice';
import {type ElementTreeItemTitleProps} from './element-tree-item-title-props';
import {type AnyElement} from '../../models/elements/any-element';
import {getStepIcon} from '../../data/step-icons';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import KeyboardArrowDownOutlinedIcon from '@mui/icons-material/KeyboardArrowDownOutlined';
import KeyboardArrowRightOutlinedIcon from '@mui/icons-material/KeyboardArrowRightOutlined';
import {getElementIcon} from '../../data/element-type/element-icons';
import {ElementStatusIcons} from './element-status-icons';

const highlightOutlineStyle = '#86FFD388 solid 2px';
const highlightBoxShadowStyle = '0px 4px 20px rgba(179, 242, 219, 0.5)';

export function ElementTreeItemTitle<T extends AnyElement>(props: ElementTreeItemTitleProps<T>) {
    const testMode = useAppSelector(selectUseTestMode);
    const useIdsInComponentTree = useAppSelector(selectUseIdsInComponentTree);
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
            onMouseDown={removeHighlightElement}
            onMouseUp={addHighlightElement}
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
                    <Box sx={{pl: 4.5}} />
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
                <ElementStatusIcons element={props.element} />

                {
                    props.editable &&
                    (props.onShowAddDialog != null) &&
                    <Tooltip
                        title="Element hinzufügen"
                        arrow
                    >
                        <IconButton onClick={props.onShowAddDialog}>
                            <AddCircleOutlineOutlinedIcon />
                        </IconButton>
                    </Tooltip>
                }
            </div>
        </div>
    );
}
