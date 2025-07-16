import React, {PropsWithChildren, useState} from 'react';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Tab from '@mui/material/Tab';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableRow from '@mui/material/TableRow';
import Tabs from '@mui/material/Tabs';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectDevToolsTab, setDevToolsTab} from '../../slices/admin-settings-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {Actions} from '../actions/actions';
import {Action} from '../actions/actions-props';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import CloseIcon from '@mui/icons-material/Close';
import {format} from 'date-fns';
import {downloadObjectFile} from '../../utils/download-utils';
import type {CustomerInput} from '../../models/customer-input';
import {isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {LogLevel, selectLogLevel, selectLogs, setLogLevel} from '../../slices/logging-slice';
import {LogLevelIcon} from '../log-level-icon/log-level-icon';
import {DragHandleOutlined} from '@mui/icons-material';
import {ElementData} from '../../models/element-data';
import {AnyElement} from '../../models/elements/any-element';
import {ElementDataDebugger} from './element-data-debugger/element-data-debugger';
import {selectLoadedForm} from '../../slices/app-slice';

interface TabContentProps {
    selectedTab: number;
    index: number;
    actions?: Action[];
}

function TabContent(props: PropsWithChildren<TabContentProps>) {
    if (props.selectedTab !== props.index) {
        return null;
    }

    return (
        <Box>
            {
                props.actions != null &&
                <Box
                    sx={{
                        display: 'flex',
                        justifyContent: 'flex-end',
                    }}
                >
                    <Actions actions={props.actions} />
                </Box>
            }
            <Box>
                {props.children}
            </Box>
        </Box>
    );
}

interface DeveloperToolsProps {
    rootElement: AnyElement;
    elementData: ElementData;
}

export function DeveloperTools(props: DeveloperToolsProps) {
    const {
        rootElement,
        elementData,
    } = props;

    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);
    const tab = useAppSelector(selectDevToolsTab);

    const currentLogLevel = useAppSelector(selectLogLevel);
    const logs = useAppSelector(selectLogs(currentLogLevel));

    const [isCollapsed, setIsCollapsed] = useState(false);
    const [height, setHeight] = useState(300);
    const [isResizing, setIsResizing] = useState(false);

    const handleExport = (): void => {
        const filename = `nutzereingaben-${form?.slug}_${format(new Date(), 'dd-MM-yyyy')}.json`;
        const input = cleanCustomerInput(elementData);
        downloadObjectFile(filename, input);
    };

    const startResize = (event: React.MouseEvent) => {
        setIsResizing(true);

        const startY = event.clientY;
        const startHeight = height;

        const handleMouseMove = (moveEvent: MouseEvent) => {
            const newHeight = startHeight + (startY - moveEvent.clientY);
            if (newHeight > 100 && newHeight < window.innerHeight * 0.8) {
                setHeight(newHeight);
            }
        };

        const handleMouseUp = () => {
            setIsResizing(false);
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
    };

    if (tab === undefined) {
        return null;
    }

    return (
        <Box
            component={Paper}
            borderRadius={0}
            sx={{
                position: 'absolute',
                left: 0,
                right: 0,
                bottom: 0,
                padding: 0,
                borderTopWidth: 1,
                borderTopStyle: 'solid',
                borderTopColor: '#cfcfcf',
                zIndex: 999,
                backgroundColor: 'white',
                boxShadow: '0 0 30px rgba(0, 0, 0, .15)',
            }}
        >
            <Box
                sx={{
                    height: '10px',
                    backgroundColor: '#ededed',
                    cursor: 'ns-resize',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    '&:hover': {backgroundColor: '#cfcfcf'},
                }}
                onMouseDown={startResize}
                title={'Höhe der Entwicklerwerkzeuge anpassen'}
            >
                <DragHandleOutlined fontSize="small" />
            </Box>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                <Tabs
                    value={tab}
                    onChange={(_, newValue) => dispatch(setDevToolsTab(newValue))}
                >
                    <Tab
                        label="Element-Daten"
                        value={0}
                    />
                    <Tab
                        label="Log"
                        value={1}
                    />
                </Tabs>

                <Actions
                    sx={{
                        ml: 'auto',
                    }}
                    actions={[
                        {
                            tooltip: 'Einklappen',
                            icon: isCollapsed ? <ExpandMoreIcon /> : <ExpandLessIcon />,
                            onClick: () => setIsCollapsed(!isCollapsed),
                        },
                        {
                            tooltip: 'Schließen',
                            icon: <CloseIcon color="error" />,
                            onClick: () => {
                                dispatch(setDevToolsTab(undefined));
                            },
                        },
                    ]}
                />
            </Box>

            <Box
                sx={{
                    overflowY: 'scroll',
                    overflowX: 'scroll',
                    width: '100%',
                    height: isCollapsed ? 0 : `${height}px`,
                    transition: isCollapsed ? 'height 0.3s ease-in-out' : 'none',
                    padding: isCollapsed ? 0 : 2,
                }}
            >

                <TabContent
                    selectedTab={tab}
                    index={0}
                >
                    <ElementDataDebugger
                        rootElement={rootElement}
                        elementData={elementData}
                        onLoadElementData={loadedData => {
                            console.log(loadedData); // TODO
                        }}
                    />
                </TabContent>

                <TabContent
                    selectedTab={tab}
                    index={1}
                    actions={[
                        {
                            tooltip: 'Debug',
                            icon: <LogLevelIcon
                                level={LogLevel.Debug}
                                active={currentLogLevel <= LogLevel.Debug}
                            />,
                            onClick: () => dispatch(setLogLevel(LogLevel.Debug)),
                        },
                        {
                            tooltip: 'Info',
                            icon: <LogLevelIcon
                                level={LogLevel.Info}
                                active={currentLogLevel <= LogLevel.Info}
                            />,
                            onClick: () => dispatch(setLogLevel(LogLevel.Info)),
                        },
                        {
                            tooltip: 'Warnung',
                            icon: <LogLevelIcon
                                level={LogLevel.Warning}
                                active={currentLogLevel <= LogLevel.Warning}
                            />,
                            onClick: () => dispatch(setLogLevel(LogLevel.Warning)),
                        },
                        {
                            tooltip: 'Fehler',
                            icon: <LogLevelIcon
                                level={LogLevel.Error}
                                active={currentLogLevel <= LogLevel.Error}
                            />,
                            onClick: () => dispatch(setLogLevel(LogLevel.Error)),
                        },
                    ]}
                >
                    <TableContainer>
                        <Table>
                            <TableBody>
                                {
                                    logs.slice().reverse().map((log, index) => (
                                        <TableRow
                                            key={log.timestamp.toString() + index}
                                        >
                                            <TableCell>
                                                <LogLevelIcon
                                                    level={log.type}
                                                    active={true}
                                                />
                                            </TableCell>
                                            <TableCell>
                                                {format(log.timestamp, 'HH:mm:ss')}
                                            </TableCell>
                                            <TableCell>
                                                {log.source}
                                            </TableCell>
                                            <TableCell>
                                                {log.message}
                                            </TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </TableContainer>
                </TabContent>
            </Box>
        </Box>
    );
}

function cleanCustomerInput(input: CustomerInput): CustomerInput {
    const cleanedInput: CustomerInput = {};
    for (const key of Object.keys(input)) {
        const value = input[key];
        if (Array.isArray(value) && value.length > 0 && isFileUploadElementItem(value[0])) {

        } else {
            cleanedInput[key] = value;
        }
    }
    return cleanedInput;
}