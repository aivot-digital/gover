import React, { useEffect, useState } from 'react';
import { Typography } from '@mui/material';
import { type BaseEditorProps } from '../../editors/base-editor';
import { type RootElement } from '../../models/elements/root-element';
import { type Destination } from '../../models/entities/destination';
import { DestinationsService } from '../../services/destinations-service';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { SelectFieldComponent } from '../select-field/select-field-component';
import { AlertComponent } from '../alert/alert-component';
import { showErrorSnackbar } from '../../slices/snackbar-slice';
import { DestinationType } from '../../data/destination-type/destination-type';

export function RootComponentEditorTabSchnittstellen(props: BaseEditorProps<RootElement>): JSX.Element {
    const dispatch = useAppDispatch();
    const [destinations, setDestinations] = useState<Destination[]>();

    useEffect(() => {
        DestinationsService
            .list()
            .then(setDestinations)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Schnittstellen konnte nicht geladen werden.'));
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
                <SelectFieldComponent
                    label="Auswahl der Schnittstelle"
                    value={ props.application.destination?.toString() ?? undefined }
                    onChange={ (val) => {
                        props.onPatchApplication({
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
                props.application.destination == null &&
                <AlertComponent
                    title="Keine Schnittstelle ausgewählt"
                    color="info"
                >
                    Sie haben aktuell keine Schnittstelle ausgewählt. Bitte beachten Sie, dass in diesem Fall die
                    Anträge ausschließlich in Gover eingehen. Die Mitarbeiter:innen des bewirtschaftenden oder
                    zuständigen Fachbereichs werden per E-Mail über eingegangene Anträge informiert.
                </AlertComponent>
            }

            {
                props.application.destination != null &&
                destinations?.find((dest) => dest.id === props.application.destination)?.type === DestinationType.Mail &&
                <AlertComponent
                    title="Hinweis zur E-Mail Schnittstelle"
                    color="warning"
                >
                    Auch wenn der Transportweg der E-Mail verschlüsselt ist, sind die Inhalte der E-Mail mitsamt
                    eventueller Anhänge nicht verschlüsselt. Bitte prüfen Sie, in Abwägung mit den von Ihnen im
                    vorliegenden Fall verarbeiteten Datenkategorien, ob die E-Mail Schnittstelle für die Übertragung der
                    Daten geeignet ist.
                </AlertComponent>
            }
        </>
    );
}
