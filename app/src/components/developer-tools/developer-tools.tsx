import React, {PropsWithChildren} from 'react';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectDevToolsTab, setDevToolsTab} from '../../slices/admin-settings-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {Actions} from '../actions/actions';
import {Action} from '../actions/actions-props';
import CloseIcon from '@mui/icons-material/Close';
import {LogLevel, selectLogLevel, setLogLevel} from '../../slices/logging-slice';
import {LogLevelIcon} from '../log-level-icon/log-level-icon';
import {AuthoredElementValues} from '../../models/element-data';
import {AnyElement} from '../../models/elements/any-element';
import {ElementDataDebugger} from './tabs/element-data-debugger';
import {selectLoadedForm} from '../../slices/app-slice';
import {LogView} from './tabs/log-view';

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
    dataLabel: string;
    rootElement: AnyElement;
    elementData: AuthoredElementValues;
    onElementDataChange: (data: AuthoredElementValues) => void;
}

export function DeveloperTools(props: DeveloperToolsProps) {
    const {
        dataLabel,
        rootElement,
        elementData,
        onElementDataChange,
    } = props;

    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);
    const tab = useAppSelector(selectDevToolsTab);

    const currentLogLevel = useAppSelector(selectLogLevel);

    if (tab === undefined) {
        return null;
    }

    return (
        <Box
            component={Paper}
            borderRadius={0}
            sx={{
                borderTopWidth: 1,
                borderTopStyle: 'solid',
                borderTopColor: '#cfcfcf',
                zIndex: 999,
                backgroundColor: 'white',
                boxShadow: '0 0 30px rgba(0, 0, 0, .15)',
                height: '100%',
            }}
        >
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
                    height: `100%`,
                    padding: 2,
                }}
            >

                <TabContent
                    selectedTab={tab}
                    index={0}
                >
                    <ElementDataDebugger
                        dataLabel={dataLabel}
                        rootElement={rootElement}
                        elementData={elementData}
                        onLoadElementData={onElementDataChange}
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
                    <LogView />
                </TabContent>
            </Box>
        </Box>
    );
}
