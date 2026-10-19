import {
    type OrganizationChartDepartmentItem,
    type OrganizationChartUserItem,
} from './organization-chart-types';

export function sortOrganizationChartDepartmentTrees(departments: OrganizationChartDepartmentItem[]): void {
    departments.sort(compareNamedItems);

    for (const department of departments) {
        department.members.sort(compareOrganizationChartUsers);
        sortOrganizationChartDepartmentTrees(department.children);
    }
}

export function compareNamedItems<T extends {name: string}>(a: T, b: T): number {
    return a.name.localeCompare(b.name, 'de', {sensitivity: 'base'});
}

export function compareOrganizationChartUsers(a: OrganizationChartUserItem, b: OrganizationChartUserItem): number {
    return getOrganizationChartUserSortName(a).localeCompare(getOrganizationChartUserSortName(b), 'de', {sensitivity: 'base'});
}

function getOrganizationChartUserSortName(user: OrganizationChartUserItem): string {
    const fullName = `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim();
    if (fullName.length > 0) {
        return fullName;
    }

    return user.fullName ?? user.email ?? user.id;
}
