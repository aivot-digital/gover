import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {useState} from "react";
import {useParams} from "react-router-dom";
import {Tab, Tabs} from "@mui/material";
import {EditDepartmentPageCommonTab} from "./tabs/edit-department-page-common-tab";
import {EditDepartmentPageMembersTab} from "./tabs/edit-department-page-members-tab";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {UserRole} from "../../../data/user-role";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";

export function DepartmentEditPage() {
    const {id} = useParams();

    useAuthGuard();
    useUserGuard((user, memberships) => (user != null && user.admin) || (memberships != null && id != null && memberships.some(mem => mem.department === parseInt(id) && mem.role === UserRole.Admin)));

    const [currentTab, setCurrentTab] = useState(0);

    return (
        <PageWrapper
            title="Fachbereich bearbeiten"
        >
            <Tabs
                value={currentTab}
                onChange={(_, val) => setCurrentTab(val)}
                sx={{
                    mb: 2,
                    borderBottom: 1,
                    borderColor: 'divider',
                }}
            >
                <Tab
                    value={0}
                    label="Allgemein"
                />
                {
                    id !== 'new' &&
                    <Tab
                        value={1}
                        label="Mitarbeiter:innen"
                    />
                }
            </Tabs>

            {
                currentTab === 0 &&
                <EditDepartmentPageCommonTab id={id ?? 'new'}/>
            }

            {
                id !== 'new' &&
                currentTab === 1 &&
                <EditDepartmentPageMembersTab id={id ?? 'new'}/>
            }
        </PageWrapper>
    );
}