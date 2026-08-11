import {Paper, styled} from '@mui/material';

/**
 * Keeps the compact editor-card radius while retaining MUI Paper's regular elevation treatment.
 */
export const DashboardPanel = styled(Paper)(() => ({
    backgroundImage: 'none',
    borderRadius: '6px',
}));
