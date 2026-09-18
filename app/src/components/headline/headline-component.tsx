import {Typography, useTheme} from '@mui/material';
import Balancer from 'react-wrap-balancer';

export function HeadlineComponent({
                                      content,
                                      small,
                                      uppercase,
                                  }: {content: string, small: boolean, uppercase?: boolean}) {
    const theme = useTheme();
    return (
        <Typography
            variant={small ? 'h5' : 'h4'}
            component={'h3'} // we set h3 explicitly to avoid a user creating a wrong headline hierarchy (A11y)
            className={"headline-component-content"}
            sx={{
                fontSize: small ? "1.125rem" : "1.25rem",
                textTransform: uppercase ? "uppercase" : "none",
                margin: small ? '0.5rem 0 0.5rem 0' : '1.25rem 0 0.5rem 0',
                maxWidth: '660px',
            }}
        >
            <Balancer>
                {content}
            </Balancer>
        </Typography>
    );
}
