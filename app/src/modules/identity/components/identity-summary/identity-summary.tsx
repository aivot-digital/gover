import React, {useEffect, useMemo, useState} from 'react';
import {Form} from '../../../../models/entities/form';
import {SubmissionDetailsResponseDTO} from '../../../submissions/dtos/submission-details-response-dto';
import {StatusTable} from '../../../../components/status-table/status-table';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {IdentityCustomerInputKey} from '../../constants/identity-customer-input-key';
import {IdentityValue} from '../../models/identity-value';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {useApi} from '../../../../hooks/use-api';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import SubdirectoryArrowRightOutlinedIcon from '@mui/icons-material/SubdirectoryArrowRightOutlined';
import {isStringNullOrEmpty} from '../../../../utils/string-utils';

interface IdentitySummaryProps {
    submission: SubmissionDetailsResponseDTO;
}

export function IdentitySummary(props: IdentitySummaryProps) {
    const api = useApi();

    const {
        submission,
    } = props;

    const [identityProvider, setIdentityProvider] = useState<IdentityProviderDetailsDTO>();

    const identityValue: IdentityValue | undefined = useMemo(() => {
        return submission.customerInput[IdentityCustomerInputKey];
    }, [submission]);

    useEffect(() => {
        if (identityValue == null) {
            setIdentityProvider(undefined);
            return;
        }

        new IdentityProvidersApiService(api)
            .retrieve(identityValue.identityProviderKey)
            .then(setIdentityProvider)
            .catch(console.error);
    }, [identityValue]);

    const rows = useMemo(() => {
        if (identityValue == null || identityProvider == null) {
            return [];
        }

        return createAuthRows(identityValue, identityProvider);
    }, [identityValue, identityProvider]);

    if (identityValue == null) {
        return null;
    }

    return (
        <StatusTable
            label="Servicekonto"
            items={rows}
        />
    );
}

function createAuthRows(identityValue: IdentityValue, identityProvider: IdentityProviderDetailsDTO) {
    const rows = [{
        icon: <CheckCircleOutlineOutlinedIcon color="success" />,
        label: 'Authentifizierung',
        children: `Authentifiziert via ${identityProvider.name}`,
    }];

    for (const attribute of identityProvider.attributes) {
        if (!attribute.displayAttribute) {
            continue;
        }

        let value = identityValue.userInfo[attribute.keyInData];
        if (isStringNullOrEmpty(value)) {
            value = 'Keine Angaben';
        }

        rows.push({
            icon: <SubdirectoryArrowRightOutlinedIcon />,
            label: attribute.label,
            children: value,
        });
    }

    return rows;
}
