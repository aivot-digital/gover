import React, { type FormEvent, type PropsWithChildren, useState } from 'react';
import { type FormPageWrapperProps } from './form-page-wrapper-props';
import { PageWrapper } from '../page-wrapper/page-wrapper';
import { Box, Button } from '@mui/material';
import { ConfirmDialog } from '../../dialogs/confirm-dialog/confirm-dialog';

export function FormPageWrapper (props: PropsWithChildren<FormPageWrapperProps>): JSX.Element {
    const {
        hasChanged,
        onSave,
        onReset,
        onDelete,

        children,

        ...pageWrapperProps
    } = props;

    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        onSave();
    };

    const handleDelete = (): void => {
        setConfirmDelete(() => onDelete);
    };

    return (
        <PageWrapper { ...pageWrapperProps }>
            <form onSubmit={ handleSubmit }>
                { children }

                <Box
                    sx={ {
                        mt: 4,
                        display: 'flex',
                    } }
                >
                    <Button
                        type="submit"
                        disabled={ !hasChanged }
                    >
                        Speichern
                    </Button>

                    {
                        onReset != null &&
                        <Button
                            sx={ { ml: 2 } }
                            type="reset"
                            color="error"
                            disabled={ !hasChanged }
                            onClick={ onReset }
                        >
                            Zurücksetzen
                        </Button>
                    }

                    {
                        onDelete != null &&
                        <Button
                            sx={ {
                                ml: 'auto',
                            } }
                            type="button"
                            color="error"
                            onClick={ handleDelete }
                        >
                            Löschen
                        </Button>
                    }
                </Box>
            </form>

            <ConfirmDialog
                title="Löschen"
                onCancel={ () => {
                    setConfirmDelete(undefined);
                } }
                onConfirm={ confirmDelete }
            >
                Soll dieses Element wirklich gelöscht werden? <strong>Achtung!</strong> Dies kann nicht rückgängig
                gemacht werden.
            </ConfirmDialog>
        </PageWrapper>
    );
}
