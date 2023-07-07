import { useAuthGuard } from '../../../hooks/use-auth-guard';
import React, { type FormEvent, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { PageWrapper } from '../../../components/page-wrapper/page-wrapper';
import { FileUpload } from '../../../components/file-upload/file-upload';
import { AssetService } from '../../../services/asset-service';
import { showErrorSnackbar } from '../../../slices/snackbar-slice';

export function AssetEditPage(): JSX.Element {
    useAuthGuard();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {name} = useParams();

    const [file, setFile] = useState<File[]>([]);
    const [isBusy, setIsBusy] = useState(false);

    const handleSubmit = (event: FormEvent): void => {
        event.preventDefault();

        if (file.length === 0) {
            return;
        }

        const form = new FormData();
        form.set('file', file[0]);

        setIsBusy(true);
        AssetService
            .create(form)
            .then(() => {
                navigate('/assets');
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die von Ihnen hochgeladene Datei weist die Signatur eines Virus auf und wurde abgelehnt. Probieren Sie eine andere Datei.'));
                setIsBusy(false);
            });
    };

    const handleDelete = (): void => {
        if (name != null) {
            setIsBusy(true);
            AssetService
                .destroy(name)
                .then(() => {
                    navigate('/assets');
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Datei konnte nicht gelöscht werden.'));
                    setIsBusy(false);
                });
        }
    };

    return (
        <PageWrapper
            title="Dokument / Medieninhalt bearbeiten"
            isLoading={ isBusy }
        >
            {
                name != null &&
                name === 'new' &&
                <form onSubmit={ handleSubmit }>
                    <Typography
                        variant="h6"
                        sx={ {mb: 4} }
                    >
                        Neue Anlage hochladen
                    </Typography>

                    <FileUpload
                        value={ file }
                        onChange={ setFile }
                        multiple={ false }
                    />

                    <Box sx={ {mt: 4} }>
                        <Button
                            type="submit"
                            disabled={ file.length === 0 }
                        >
                            Hochladen
                        </Button>
                    </Box>
                </form>
            }

            {
                name != null &&
                name !== 'new' &&
                <Box>
                    <Typography>
                        Link zum Dokument / Medieninhalt:&nbsp;
                        <a
                            href={ AssetService.getLink(name) }
                            target="_blank"
                            rel="noreferrer"
                        >
                            { AssetService.getLink(name) }
                        </a>
                    </Typography>

                    <Box sx={ {mt: 4} }>
                        <Button
                            color="error"
                            onClick={ handleDelete }
                        >
                            Dokument / Medieninhalt Löschen
                        </Button>
                    </Box>
                </Box>
            }
        </PageWrapper>
    );
}
