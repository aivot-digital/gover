import React, { useEffect, useState } from 'react';
import { Typography } from '@mui/material';
import { type BaseEditorProps } from '../../editors/base-editor';
import { type RootElement } from '../../models/elements/root-element';
import { type Destination } from '../../models/entities/destination';
import { DestinationsService } from '../../services/destinations-service';
import { useAppSelector } from '../../hooks/use-app-selector';
import { selectLoadedApplication, updateAppModel } from '../../slices/app-slice';
import { type Application } from '../../models/entities/application';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { SelectFieldComponent } from '../select-field/select-field-component';
import { AlertComponent } from '../alert/alert-component';

export function RootComponentEditorTabSchnittstellen(props: BaseEditorProps<RootElement>) {
    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);
    const [destinations, setDestinations] = useState<Destination[]>();

    useEffect(() => {
        DestinationsService
            .list()
            .then(setDestinations);
    }, []);

    const handleApplicationPatch = (patch: Partial<Application>) => {
        if (application == null) {
            return;
        }

        dispatch(updateAppModel({
            ...application,
            ...patch,
        }));
    };

    return (
        <>
            <Typography
                variant="h6"
            >
                Schnittstellen
            </Typography>

            {
                destinations != null &&
                <SelectFieldComponent
                    label="Auswahl der Schnittstelle"
                    value={ application?.destination?.toString() ?? undefined }
                    onChange={ (val) => {
                        handleApplicationPatch({
                            destination: val != null ? parseInt(val) : undefined,
                        });
                    } }
                    options={ destinations.map((destination) => ({
                        value: destination.id.toString(),
                        label: destination.name,
                    })) }
                />
            }

            {
                application?.destination == null &&
                <AlertComponent
                    title="Keine Schnittstelle ausgewählt"
                    color="info"
                >
                    Sie haben aktuell keine Schnittstelle ausgewählt. Bitte beachten Sie, dass in diesem Fall die
                    Anträge ausschließlich in Gover eingehen. Die Mitarbeiter:innen des bewirtschaftenden oder
                    zuständigen Fachbereichs werden per E-Mail über eingegangene Anträge informiert.
                </AlertComponent>
            }
        </>
    );
}
