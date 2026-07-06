import {
    type OrganigramDepartmentItem,
    type OrganigramUserItem,
} from './organigram-types';

export function sortOrganigramDepartmentTrees(departments: OrganigramDepartmentItem[]): void {
    departments.sort(compareNamedItems);

    for (const department of departments) {
        department.members.sort(compareOrganigramUsers);
        sortOrganigramDepartmentTrees(department.children);
    }
}

export function compareNamedItems<T extends {name: string}>(a: T, b: T): number {
    return a.name.localeCompare(b.name, 'de', {sensitivity: 'base'});
}

export function compareOrganigramUsers(a: OrganigramUserItem, b: OrganigramUserItem): number {
    return getOrganigramUserSortName(a).localeCompare(getOrganigramUserSortName(b), 'de', {sensitivity: 'base'});
}

function getOrganigramUserSortName(user: OrganigramUserItem): string {
    const fullName = `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim();
    if (fullName.length > 0) {
        return fullName;
    }

    return user.fullName ?? user.email ?? user.id;
}
