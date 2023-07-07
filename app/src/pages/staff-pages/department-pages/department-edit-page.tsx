import { useAuthGuard } from '../../../hooks/use-auth-guard';
import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Tab, Tabs } from '@mui/material';
import { EditDepartmentPageCommonTab } from './tabs/edit-department-page-common-tab';
import { EditDepartmentPageMembersTab } from './tabs/edit-department-page-members-tab';
import { useUserGuard } from '../../../hooks/use-user-guard';
import { UserRole } from '../../../data/user-role';
import { PageWrapper } from '../../../components/page-wrapper/page-wrapper';
import { type Department } from '../../../models/entities/department';
import { DepartmentsService } from '../../../services/departments-service';
import { delayPromise, withDelay } from '../../../utils/with-delay';
import { showErrorSnackbar, showSuccessSnackbar } from '../../../slices/snackbar-slice';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { shallowEquals } from '../../../utils/equality-utils';

export function DepartmentEditPage(): JSX.Element {
    const { id } = useParams();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    useAuthGuard();
    useUserGuard((user, memberships) => (
        (user?.admin ?? false) ||
        (memberships != null && id != null && memberships.some((mem) => mem.department === parseInt(id) && mem.role === UserRole.Admin))
    ));

    const [originalDepartment, setOriginalDepartment] = useState<Department>();
    const [department, setDepartment] = useState<Department>();

    const [currentTab, setCurrentTab] = useState(0);
    const [isBusy, setIsBusy] = useState(true);
    const [is404, setIs404] = useState(false);

    const hasChanged = !shallowEquals(originalDepartment, department);

    useEffect(() => {
        setIsBusy(true);
        setIs404(false);
        if (id == null || id === 'new') {
            const newDepartment: Department = {
                id: 0,
                name: '',
                address: '',
                accessibility: '',
                imprint: '',
                privacy: '',
                specialSupportAddress: '',
                technicalSupportAddress: '',
                created: '',
                updated: '',
            };
            setOriginalDepartment(newDepartment);
            setDepartment(newDepartment);
            setIsBusy(false);
        } else {
            delayPromise(DepartmentsService.retrieve(parseInt(id)))
                .then((user) => {
                    setOriginalDepartment(user);
                    setDepartment(user);
                })
                .catch((err) => {
                    console.error(err);
                    setIs404(true);
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    }, [id]);

    const handleChange = (patch: Partial<Department>): void => {
        if (department != null) {
            setDepartment({
                ...department,
                ...patch,
            });
        }
    };

    const handleSave = (): void => {
        if (department != null) {
            setIsBusy(true);

            if (department.id === 0) {
                DepartmentsService
                    .create(department)
                    .then((createdDepartment) => {
                        dispatch(showSuccessSnackbar('Fachbereich erfolgreich erstellt!'));
                        navigate(`/departments/${ createdDepartment.id }`);
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Fachbereich konnte nicht gespeichert werden!'));
                        setIsBusy(false);
                    });
            } else {
                DepartmentsService
                    .update(department.id, department)
                    .then((updatedDepartment) => {
                        setOriginalDepartment(updatedDepartment);
                        setDepartment(updatedDepartment);
                        dispatch(showSuccessSnackbar('Fachbereich erfolgreich gespeichert!'));
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Fachbereich konnte nicht gespeichert werden!'));
                        setIsBusy(false);
                    });
            }
        }
    };

    const handleReset = (): void => {
        if (originalDepartment != null) {
            setDepartment(originalDepartment);
        }
    };

    const handleDelete = (): void => {
        if (department != null && department.id !== 0) {
            setIsBusy(true);

            DepartmentsService
                .destroy(department.id)
                .then(() => {
                    navigate('/departments');
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Fachbereich konnte nicht gelöscht werden!'));
                    setIsBusy(false);
                });
        }
    };

    return (
        <PageWrapper
            title="Fachbereich bearbeiten"
            isLoading={ isBusy }
            is404={ is404 }
        >
            <Tabs
                value={ currentTab }
                onChange={ (_, val) => {
                    setCurrentTab(val);
                } }
                sx={ {
                    mb: 2,
                    borderBottom: 1,
                    borderColor: 'divider',
                } }
            >
                <Tab
                    value={ 0 }
                    label="Allgemein"
                />
                {
                    id !== 'new' &&
                    <Tab
                        value={ 1 }
                        label="Mitarbeiter:innen"
                    />
                }
            </Tabs>

            {
                department != null &&
                currentTab === 0 &&
                <EditDepartmentPageCommonTab
                    department={ department }
                    hasChanged={ hasChanged }
                    onChange={ handleChange }
                    onSave={ handleSave }
                    onReset={ handleReset }
                    onDelete={ handleDelete }
                />
            }

            {
                department != null &&
                department.id !== 0 &&
                currentTab === 1 &&
                <EditDepartmentPageMembersTab
                    department={ department }
                />
            }
        </PageWrapper>
    );
}
