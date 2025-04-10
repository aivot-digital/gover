import {ReactNode, useEffect, useState} from 'react';
import {Box, Button} from '@mui/material';
import {useApi} from '../../../hooks/use-api';
import {SecretsApiService} from '../secrets-api-service';
import {Secret} from '../models/secret';
import {SecretSelectDialog} from '../dialogs/secret-select-dialog';

export interface SecretSelectComponentProps {
    label: string;
    hint?: string;
    error?: string;
    value: string;
    onChange: (key: string) => void;
}

export function SecretSelectComponent(props: SecretSelectComponentProps): ReactNode {
    const api = useApi();
    const [selectedSecret, setSelectedSecret] = useState<Secret>();
    const [showDialog, setShowDialog] = useState(false);

    const secretKey = props.value;

    useEffect(() => {
        if (secretKey == null) {
            setSelectedSecret(undefined);
        } else {
            new SecretsApiService(api)
                .retrieve(secretKey)
                .then(setSelectedSecret);
        }
    }, [secretKey]);

    return (
        <>
            <Box>
                {selectedSecret != null ? selectedSecret.name : 'Loading...'}

                <Button onClick={() => setShowDialog(true)}>
                    Select
                </Button>
            </Box>

            <SecretSelectDialog
                open={showDialog}
                onClose={() => setShowDialog(false)}
            />
        </>
    );
}