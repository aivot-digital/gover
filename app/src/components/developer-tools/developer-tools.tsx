import React, {PropsWithChildren, useState} from 'react';
import {Box, Paper, Tab, Table, TableBody, TableCell, TableContainer, TableRow, Tabs, Typography} from '@mui/material';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectDevToolsTab, setDevToolsTab} from '../../slices/admin-settings-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {Actions} from '../actions/actions';
import {Action} from '../actions/actions-props';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import CloseIcon from '@mui/icons-material/Close';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import {format} from 'date-fns';
import {downloadObjectFile, uploadObjectFile} from '../../utils/download-utils';
import type {CustomerInput} from '../../models/customer-input';
import {hydrateCustomerInput, selectLoadedForm, selectFunctionReferences} from '../../slices/app-slice';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {LogLevel, selectLogLevel, selectLogs, setLogLevel} from '../../slices/logging-slice';
import {LogLevelIcon} from '../log-level-icon/log-level-icon';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {FunctionType} from '../../utils/function-status-utils';
import {DragHandleOutlined} from "@mui/icons-material";
import {DeveloperToolsTabVisibilities} from './developer-tools-tab-visiblities';

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

export function DeveloperTools() {
    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);
    const tab = useAppSelector(selectDevToolsTab);
    const userInput = useAppSelector(state => state.app.inputs);
    const values = useAppSelector(state => state.app.values);
    const currentLogLevel = useAppSelector(selectLogLevel);
    const logs = useAppSelector(selectLogs(currentLogLevel));
    const references = useAppSelector(selectFunctionReferences);

    const [isCollapsed, setIsCollapsed] = useState(false);
    const [height, setHeight] = useState(300);
    const [isResizing, setIsResizing] = useState(false);

    const handleExport = (): void => {
        const filename = `nutzereingaben-${form?.slug}_${format(new Date(), 'dd-MM-yyyy')}.json`;
        const input = cleanCustomerInput(userInput);
        downloadObjectFile(filename, input);
    };

    const handleUpload = (): void => {
        uploadObjectFile<CustomerInput>('.json')
            .then((res) => {
                if (res == null) {
                    dispatch(hydrateCustomerInput({}));
                } else {
                    dispatch(hydrateCustomerInput(res));
                }
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Nutzereingaben konnten nicht geladen werden'));
            });
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
            document.removeEventListener("mousemove", handleMouseMove);
            document.removeEventListener("mouseup", handleMouseUp);
        };

        document.addEventListener("mousemove", handleMouseMove);
        document.addEventListener("mouseup", handleMouseUp);
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
                    '&:hover': { backgroundColor: '#cfcfcf' },
                }}
                onMouseDown={startResize}
                title={"Höhe der Entwicklerwerkzeuge anpassen"}
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
                        label="Nutzereingaben"
                        value={0}
                    />
                    <Tab
                        label="Sichtbarkeiten"
                        value={1}
                    />
                    <Tab
                        label="Abhängigkeiten"
                        value={2}
                    />
                    <Tab
                        label="Log"
                        value={3}
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
                    actions={[
                        {
                            tooltip: 'Exportieren',
                            label: 'Exportieren',
                            icon: <FileDownloadOutlinedIcon />,
                            onClick: handleExport,
                        },
                        {
                            tooltip: 'Importieren',
                            label: 'Importieren',
                            icon: <UploadFileOutlinedIcon />,
                            onClick: handleUpload,
                        },
                    ]}
                >
                    <Typography>
                        Eingaben:
                    </Typography>

                    <Box component="code">
                        <Box component="pre">{
                            JSON.stringify(userInput, null, 4)
                        }</Box>
                    </Box>

                    <Typography>
                        Berechnet:
                    </Typography>

                    <Box component="code">
                        <Box component="pre">{
                            JSON.stringify(values, null, 4)
                        }</Box>
                    </Box>
                </TabContent>

                <TabContent
                    selectedTab={tab}
                    index={1}
                >
                    <DeveloperToolsTabVisibilities/>
                </TabContent>

                <TabContent
                    selectedTab={tab}
                    index={2}
                >
                    {
                        references != null &&
                        form?.root?.children != null &&
                        form
                            .root
                            .children
                            .map(step => (
                                <Box
                                    key={step.id}
                                    sx={{
                                        mb: 3,
                                    }}
                                >
                                    <Typography variant="subtitle1">
                                        {generateComponentTitle(step)} (ID: {step.id})
                                    </Typography>

                                    <Box
                                        sx={{
                                            ml: 2,
                                            pl: 2,
                                            borderLeft: '1px solid gray',
                                        }}
                                    >
                                        {
                                            references.every(reference => reference.sourceStep.id !== step.id) &&
                                            <Typography>
                                                Keine Abhängigkeiten in diesem Abschnitt
                                            </Typography>
                                        }

                                        {
                                            references.some(reference => reference.sourceStep.id === step.id) &&
                                            <>
                                                {
                                                    references
                                                        .filter(reference => reference.sourceStep.id === step.id)
                                                        .map(({source, target, functionType, isSameStep}) => (
                                                            <Box
                                                                key={`${source.id}-${target.id}-${functionType}`}
                                                                sx={{
                                                                    mb: 1,
                                                                }}
                                                            >
                                                                {
                                                                    functionType === FunctionType.OVERRIDE &&
                                                                    <Typography>
                                                                        Die Elementstruktur von <strong>{generateComponentTitle(source)}</strong> (ID: {source.id}) hängt von <strong>{generateComponentTitle(target)}</strong> (ID: {target.id}) ab.
                                                                    </Typography>
                                                                }

                                                                {
                                                                    functionType === FunctionType.VALIDATION &&
                                                                    <Typography>
                                                                        Die Validierung von <strong>{generateComponentTitle(source)}</strong> (ID: {source.id}) hängt von <strong>{generateComponentTitle(target)}</strong> (ID: {target.id}) ab.
                                                                    </Typography>
                                                                }

                                                                {
                                                                    functionType === FunctionType.VALUE &&
                                                                    <Typography>
                                                                        Der Wert von <strong>{generateComponentTitle(source)}</strong> (ID: {source.id}) hängt von <strong>{generateComponentTitle(target)}</strong> (ID: {target.id}) ab.
                                                                    </Typography>
                                                                }

                                                                {
                                                                    functionType === FunctionType.VISIBILITY &&
                                                                    <Typography>
                                                                        Die Sichtbarkeit von <strong>{generateComponentTitle(source)}</strong> (ID: {source.id}) hängt von <strong>{generateComponentTitle(target)}</strong> (ID: {target.id}) ab.
                                                                    </Typography>
                                                                }

                                                                {
                                                                    isSameStep &&
                                                                    <Typography>
                                                                        Die Abhängigkeit bezieht sich auf ein Element, dass sich im gleichen Abschnitt befindet.
                                                                    </Typography>
                                                                }
                                                            </Box>
                                                        ))
                                                }
                                            </>
                                        }
                                    </Box>
                                </Box>
                            ))
                    }
                </TabContent>

                <TabContent
                    selectedTab={tab}
                    index={3}
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