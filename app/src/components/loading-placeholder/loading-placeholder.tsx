import {Container} from '@mui/material';
import React, {ReactNode} from 'react';
import {Loader} from "../loader/loader";

export function LoadingPlaceholder({message}: { message?: string | ReactNode }) {
    return (
        <Container
            maxWidth="sm"
            sx={{
                py: 5.5,
            }}
        >
            <Loader message={message} />
        </Container>
    );
}