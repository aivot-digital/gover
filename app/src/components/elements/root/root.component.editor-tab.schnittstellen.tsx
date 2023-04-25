import React, {useEffect, useState} from 'react';
import {FormControl, InputLabel, MenuItem, Select, Typography} from '@mui/material';
import {RootElement} from '../../../models/elements/root-element';
import {BaseEditorProps} from '../../_lib/base-editor-props';
import {Destination} from '../../../models/entities/destination';
import {DestinationsService} from '../../../services/destinations.service';

export function RootComponentEditorTabSchnittstellen(props: BaseEditorProps<RootElement>) {
    const [destinations, setDestinations] = useState<Destination[]>();

    useEffect(() => {
        DestinationsService.list()
            .then(data => {
                setDestinations(data._embedded.destinations);
            });
    }, []);

    return (
        <>
            <Typography
                variant="h6"
            >
                Schnittstellen
            </Typography>

            {
                destinations != null &&
                <FormControl
                    margin="normal"
                >
                    <InputLabel>Auswahl der Schnittstelle</InputLabel>
                    <Select
                        label="Auswahl der Schnittstelle"
                        value={props.component.destination ?? ''}
                        onChange={event => props.onPatch({
                            destination: typeof event.target.value === 'string' ? parseInt(event.target.value) : event.target.value,
                        })}
                    >
                        {
                            destinations.map((destination) => (
                                <MenuItem
                                    key={destination.id}
                                    value={destination.id}
                                >
                                    {destination.name}
                                </MenuItem>
                            ))
                        }
                    </Select>
                </FormControl>
            }
        </>
    );
}
