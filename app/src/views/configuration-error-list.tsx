import {Box} from '@mui/material';

export function ConfigurationErrorList({messages}: {messages: string[]}) {
    return <Box component="ul" sx={{m: 0, pl: 2.5}}>
        {messages.map((message, index) => <li key={index}>{message}</li>)}
    </Box>;
}
