import React from 'react';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {Paper} from '@mui/material';
import {SystemInformation} from '../../../pages/staff-pages/settings/components/system-information/system-information';
import ReadinessScore from '@aivot/mui-material-symbols-400-n25-outlined/ReadinessScore';
import InfoOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {setShowAboutProsunaDialog} from '../../../slices/shell-slice';
import {getAboutProsunaLabel} from '../../../utils/app-info-utils';

export function SystemStatusPage() {
    const dispatch = useAppDispatch();
    const aboutProsunaLabel = getAboutProsunaLabel();

    return (
        <PageWrapper
            title="Systeminformationen"
            background={true}
        >
            <GenericPageHeader
                title="Systeminformationen"
                icon={<ReadinessScore/>}
                actions={[{
                    label: aboutProsunaLabel,
                    ariaLabel: aboutProsunaLabel,
                    icon: <InfoOutlined/>,
                    iconPosition: 'start',
                    variant: 'outlined',
                    onClick: () => dispatch(setShowAboutProsunaDialog(true)),
                }]}
            />

            <Paper
                sx={{
                    marginTop: 2.75,
                    padding: 2,
                }}
            >
                <SystemInformation />
            </Paper>
        </PageWrapper>
    );
}
