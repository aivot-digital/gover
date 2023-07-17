import React, {useReducer} from 'react';
import {Box, Button} from '@mui/material';
import {type ApplicationListItemGroupProps} from './application-list-item-group-props';
import {ApplicationListItem} from '../application-list-item/application-list-item';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faChevronDown, faChevronUp} from '@fortawesome/pro-light-svg-icons';


export function ApplicationListItemGroup({
                                             group,
                                             onClone,
                                             onDelete,
                                             onNewVersion,
                                             memberships,
                                             user,
                                         }: ApplicationListItemGroupProps): JSX.Element {
    const [isCollapsed, toggleCollapsed] = useReducer(p => !p, true);

    const [first, ...rest] = group.applications;

    return (
        <Box
            sx={{
                mb: rest.length > 0 && isCollapsed ? 4 : 8,
            }}
        >
            <ApplicationListItem
                key={first.slug + first.version}
                application={first}
                onClone={onClone}
                onDelete={onDelete}
                onNewVersion={onNewVersion}
                memberships={memberships}
                user={user}
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
                                <FontAwesomeIcon
                                    icon={isCollapsed ? faChevronDown : faChevronUp}
                                />
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
                                        onClone={onClone}
                                        onDelete={onDelete}
                                        onNewVersion={onNewVersion}
                                        memberships={memberships}
                                        user={user}
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
