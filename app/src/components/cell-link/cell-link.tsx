import {Link} from 'react-router-dom';
import {ReactNode} from 'react';
import {Box} from '@mui/material';

interface CellLinkProps {
    to: string;
    title?: string;
    children: ReactNode;
}

export function CellLink({ to, title, children }: CellLinkProps) {
    return (
        <Box
            component={Link}
            to={to}
            title={title}
            sx={{
                textDecoration: "none",
                color: "inherit",
                width: "100%",
                height: "100%",
                display: "flex",
                alignItems: "center",
                position: "relative",

                "&:hover .cell-link-text": {
                    "&::after": {
                        backgroundColor: "#ccc",
                    }
                }
            }}
        >
            <Box component={'span'}>
                <Box
                    component={'span'}
                    className="cell-link-text"
                    sx={{
                        position: "relative",
                        display: "inline",
                        whiteSpace: 'nowrap',
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        py: '4px',

                        "&::after": {
                            content: '""',
                            position: "absolute",
                            left: 0,
                            bottom: 0,
                            width: "100%",
                            height: "1px",
                            backgroundColor: "transparent",
                            transition: "background-color 0.2s ease",
                        }
                    }}
                >
                    {children}
                </Box>
            </Box>
        </Box>
    );
}
