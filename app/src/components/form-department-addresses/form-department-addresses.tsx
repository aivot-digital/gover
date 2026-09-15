import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import {FadingPaper} from '../fading-paper/fading-paper';
import {type FormLayoutElement} from '../../models/elements/form-layout-element';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {type PublicDepartmentResponseDTO} from '../../modules/departments/entities/v-department-shadowed-entity';
import {getDepartmentDisplayAddress} from '../../modules/departments/utils/department-utils';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../slices/snackbar-slice';

interface FormDepartmentAddressesProps {
    formElement?: FormLayoutElement | null;
    variant?: 'paper' | 'grid';
}

export function useFormDepartmentAddressSections(formElement?: FormLayoutElement | null): ReactNode[] {
    const responsibleDepartmentId = formElement?.responsibleDepartmentId ?? null;
    const managingDepartmentId = formElement?.managingDepartmentId ?? null;
    const responsibleDepartment = usePublicDepartment(
        responsibleDepartmentId,
        'Fehler beim Laden der zuständigen Stelle',
    );
    const managingDepartment = usePublicDepartment(
        managingDepartmentId,
        'Fehler beim Laden der bewirtschaftenden Stelle',
    );

    return useMemo(() => {
        const sections: ReactNode[] = [];
        const responsibleDepartmentAddress = getDepartmentDisplayAddress(responsibleDepartment);
        const managingDepartmentAddress = getDepartmentDisplayAddress(managingDepartment);

        if (responsibleDepartmentAddress != null) {
            sections.push(
                <Box key="responsible-department-address">
                    <Typography
                        component="h3"
                        variant="h5"
                    >
                        Zuständige Stelle
                    </Typography>
                    <Typography
                        component="pre"
                        variant="body2"
                        sx={{mt: 1}}
                    >
                        {responsibleDepartmentAddress}
                    </Typography>
                </Box>,
            );
        }

        if (managingDepartmentAddress != null) {
            sections.push(
                <Box key="managing-department-address">
                    <Typography
                        component="h3"
                        variant="h5"
                    >
                        Bewirtschaftende Stelle
                    </Typography>
                    <Typography
                        component="pre"
                        variant="body2"
                        sx={{mt: 1}}
                    >
                        {managingDepartmentAddress}
                    </Typography>
                </Box>,
            );
        }

        return sections;
    }, [responsibleDepartment, managingDepartment]);
}

function usePublicDepartment(departmentId: number | null, errorMessage: string): PublicDepartmentResponseDTO | undefined {
    const dispatch = useAppDispatch();
    const [department, setDepartment] = useState<PublicDepartmentResponseDTO>();

    useEffect(() => {
        let active = true;

        if (departmentId == null) {
            setDepartment(undefined);
            return;
        }

        setDepartment((currentDepartment) => currentDepartment?.id === departmentId ? currentDepartment : undefined);

        new DepartmentApiService()
            .retrievePublic(departmentId)
            .then((nextDepartment) => {
                if (active) {
                    setDepartment(nextDepartment);
                }
            })
            .catch((err) => {
                if (active) {
                    dispatch(showApiErrorSnackbar(err, errorMessage));
                }
            });

        return () => {
            active = false;
        };
    }, [departmentId, dispatch, errorMessage]);

    return department;
}

export function FormDepartmentAddresses(props: FormDepartmentAddressesProps) {
    const sections = useFormDepartmentAddressSections(props.formElement);
    const variant = props.variant ?? 'paper';

    if (sections.length === 0) {
        return null;
    }

    if (variant === 'grid') {
        return <DepartmentAddressGrid sections={sections}/>;
    }

    return (
        <FadingPaper>
            <DepartmentAddressSections sections={sections}/>
        </FadingPaper>
    );
}

function DepartmentAddressGrid(props: { sections: ReactNode[] }) {
    return (
        <Grid
            container
            columnSpacing={7}
            rowSpacing={3}
            sx={{mt: 4}}
        >
            {props.sections.map((section, index) => (
                <Grid
                    key={index}
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    {section}
                </Grid>
            ))}
        </Grid>
    );
}

function DepartmentAddressSections(props: { sections: ReactNode[] }) {
    return (
        <Box
            sx={{
                columnCount: {xs: 1, md: 2},
                columnGap: 7,
            }}
        >
            {props.sections.map((section, index) => (
                <Box
                    key={index}
                    sx={{
                        breakInside: 'avoid',
                        mb: 3,
                        display: 'inline-block',
                        width: '100%',
                    }}
                >
                    {section}
                </Box>
            ))}
        </Box>
    );
}
