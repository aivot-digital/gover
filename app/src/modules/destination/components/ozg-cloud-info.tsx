import {StatusTable} from '../../../components/status-table/status-table';
import React from 'react';
import { SxProps } from '@mui/material';

interface OzgCloudInfoProps {
    sx?: SxProps;
}

export function OzgCloudInfo(props: OzgCloudInfoProps) {
    return (
        <StatusTable
            label="Technisches Feld für die OZG-Cloud-Zuweisung"
            labelVariant="subtitle2"
            labelSx={{
                mb: 1,
            }}
            description="Sie müssen in den Formularen, die diese Schnittstelle verwenden, ein zusätzliches, technisches Feld für die „OrganisationsEinheitID“ der OZG-Cloud hinzufügen. Nur so können Formulare an die korrekte, zuständige Stelle in der OZG-Cloud übermittelt werden."
            descriptionSx={{
                mt: 0,
                mb: 2,
            }}
            sx={{
                mt: 3,
                mb: 4,
                ...props.sx,
            }}
            items={[
                {
                    label: 'Art des Feldes',
                    children: 'Text',
                },
                {
                    label: 'Interner Name des Feldes',
                    children: 'OZG_CLOUD_ZUSTAENDIGE_STELLE',
                },
                {
                    label: 'Titel des Feldes',
                    children: <em>Die „OrganisationsEinheitID“ der zuständigen Stelle, wie Sie in der OZG-Cloud hinterlegt
                                  ist</em>,
                },
                {
                    label: 'Technisches Feld',
                    children: 'Ja',
                },
            ]}
        />
    );
}