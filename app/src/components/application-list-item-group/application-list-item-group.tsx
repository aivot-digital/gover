import React, {useReducer} from 'react';
import {Box, Button} from '@mui/material';
import {type ApplicationListItemGroupProps} from './application-list-item-group-props';
import {ApplicationListItem} from '../application-list-item/application-list-item';
import ExpandLessOutlinedIcon from '@mui/icons-material/ExpandLessOutlined';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';

export function ApplicationListItemGroup(props: ApplicationListItemGroupProps): JSX.Element {
    const [isCollapsed, toggleCollapsed] = useReducer<(v: boolean) => boolean>((p) => !p, true);

    const [first, ...rest] = props.group.applications;

    return (
        <Box
            sx={{
                mb: rest.length > 0 && isCollapsed ? 4 : 8,
            }}
        >
            <ApplicationListItem
                key={first.slug + first.version}
                application={first}
                onClone={props.onClone}
                onDelete={props.onDelete}
                onNewVersion={props.onNewVersion}
                memberships={props.memberships}
                user={props.user}
            />

            {
                rest.length > 0 &&
                <Box
                    sx={{
                        ml: 2,
                        pl: 2,
                        borderLeftWidth: !isCollapsed ? '2px' : undefined,
                        borderLeftStyle: !isCollapsed ? 'solid' : undefined,
                        borderLeftColor: !isCollapsed ? 'divider' : undefined,
                    }}
                >
                    <Box
                        textAlign="right"
                    >
                        <Button
                            onClick={toggleCollapsed}
                            sx={{
                                textTransform: 'none',
                            }}
                            size="small"
                            endIcon={
                                isCollapsed ?
                                    <ExpandMoreOutlinedIcon/> :
                                    <ExpandLessOutlinedIcon/>
                            }
                        >
                            Ältere Versionen {isCollapsed ? 'anzeigen' : 'ausblenden'}
                        </Button>
                    </Box>

                    {
                        !isCollapsed &&
                        <Box
                            sx={{
                                mt: 2,
                            }}
                        >
                            {
                                rest.map((app) => (
                                    <ApplicationListItem
                                        key={app.slug + app.version}
                                        application={app}
                                        onClone={props.onClone}
                                        onDelete={props.onDelete}
                                        onNewVersion={props.onNewVersion}
                                        memberships={props.memberships}
                                        user={props.user}
                                    />
                                ))
                            }
                        </Box>
                    }
                </Box>
            }
        </Box>
    );
}
