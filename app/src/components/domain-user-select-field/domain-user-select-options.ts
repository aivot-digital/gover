import {VDepartmentShadowedApiService} from '../../modules/departments/services/v-department-shadowed-api-service';
import {getDepartmentPath, getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../modules/departments/utils/department-utils';
import {TeamsApiService} from '../../modules/teams/services/teams-api-service';
import {UsersApiService} from '../../modules/users/users-api-service';
import {User} from '../../modules/users/models/user';
import {createElement, ReactElement} from 'react';
import {ModuleIcons} from '../../shells/staff/data/module-icons';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import {
    DomainAndUserSelectItem,
    DomainAndUserSelectItemType,
} from '../../models/elements/form/input/domain-user-select-field-element';
import {ProcessInstanceAccessControlApiService} from '../../modules/process/services/process-instance-access-control-api-service';

export type DomainAndUserSelectOptionGroup = 'Organisationseinheiten' | 'Teams' | 'Mitarbeitende' | 'Ausgewählt';

export interface DomainAndUserSelectOption {
    key: string;
    value: DomainAndUserSelectItem;
    label: string;
    group: DomainAndUserSelectOptionGroup;
    subLabel?: string;
    icon?: ReactElement;
}

export interface DomainAndUserSelectOptionConstraint {
    processId?: number | null;
    processVersion?: number | null;
    requiredPermissions?: string[] | null;
}

interface NormalizedDomainAndUserSelectOptionConstraint {
    processId: number;
    processVersion: number;
    requiredPermissions: string[];
}

const groupOrder: Record<DomainAndUserSelectOptionGroup, number> = {
    'Organisationseinheiten': 0,
    'Teams': 1,
    'Mitarbeitende': 2,
    'Ausgewählt': 3,
};

let cachedOptions: DomainAndUserSelectOption[] | undefined;
let cachedOptionsPromise: Promise<DomainAndUserSelectOption[]> | undefined;
const PAGE_SIZE = 250;

function normalizeText(value: string | null | undefined): string {
    return value?.trim() ?? '';
}

function formatUserDisplayName(user: User): string {
    const lastName = normalizeText(user.lastName);
    const firstName = normalizeText(user.firstName);

    if (lastName.length > 0 && firstName.length > 0) {
        return `${lastName}, ${firstName}`;
    }

    if (lastName.length > 0) {
        return lastName;
    }

    if (firstName.length > 0) {
        return firstName;
    }

    const fullName = normalizeText(user.fullName);
    if (fullName.length > 0) {
        return fullName;
    }

    return `Mitarbeiter:in #${user.id}`;
}

function compareUsers(a: User, b: User): number {
    const lastNameCompare = normalizeText(a.lastName).localeCompare(normalizeText(b.lastName), 'de');
    if (lastNameCompare !== 0) {
        return lastNameCompare;
    }

    const firstNameCompare = normalizeText(a.firstName).localeCompare(normalizeText(b.firstName), 'de');
    if (firstNameCompare !== 0) {
        return firstNameCompare;
    }

    return formatUserDisplayName(a).localeCompare(formatUserDisplayName(b), 'de');
}

function sortOptions(options: DomainAndUserSelectOption[]): DomainAndUserSelectOption[] {
    return options
        .slice()
        .sort((a, b) => {
            const groupDiff = groupOrder[a.group] - groupOrder[b.group];
            if (groupDiff !== 0) {
                return groupDiff;
            }

            return a.label.localeCompare(b.label, 'de');
        });
}

export function createOrgUnitOptionValue(id: number | string): DomainAndUserSelectItem {
    return {
        type: 'orgUnit',
        id: id.toString(),
    };
}

export function createTeamOptionValue(id: number | string): DomainAndUserSelectItem {
    return {
        type: 'team',
        id: id.toString(),
    };
}

export function createUserOptionValue(id: string): DomainAndUserSelectItem {
    return {
        type: 'user',
        id: id.toString(),
    };
}

export function createDomainAndUserSelectValueKey(value: DomainAndUserSelectItem): string {
    return `${value.type}:${value.id}`;
}

export function formatDomainAndUserSelectValue(value: DomainAndUserSelectItem): string {
    if (value.type === 'orgUnit') {
        return `Organisationseinheit (ID: ${value.id})`;
    }

    if (value.type === 'team') {
        return `Team (ID: ${value.id})`;
    }

    if (value.type === 'user') {
        return `Mitarbeiter:in (ID: ${value.id})`;
    }

    return `${value.type}:${value.id}`;
}

function parseLegacyToken(value: string): DomainAndUserSelectItem | undefined {
    const normalized = value.trim();
    if (normalized.length === 0) {
        return undefined;
    }

    const firstSeparator = normalized.indexOf(':');
    if (firstSeparator < 0) {
        return undefined;
    }

    const type = normalized.slice(0, firstSeparator);
    const id = normalized.slice(firstSeparator + 1);
    if (id.length === 0) {
        return undefined;
    }

    if (type === 'orgUnit' || type === 'team' || type === 'user') {
        return {
            type,
            id,
        };
    }

    return undefined;
}

export function normalizeDomainAndUserSelectItem(value: unknown): DomainAndUserSelectItem | undefined {
    if (typeof value === 'string') {
        return parseLegacyToken(value);
    }

    if (value == null || typeof value !== 'object') {
        return undefined;
    }

    const rawType = (value as any).type;
    const rawId = (value as any).id;

    if (rawType !== 'orgUnit' && rawType !== 'team' && rawType !== 'user') {
        return undefined;
    }

    if (rawId == null) {
        return undefined;
    }

    return {
        type: rawType as DomainAndUserSelectItemType,
        id: String(rawId),
    };
}

function normalizeConstraint(constraint?: DomainAndUserSelectOptionConstraint): NormalizedDomainAndUserSelectOptionConstraint | undefined {
    if (constraint?.processId == null || constraint?.processVersion == null) {
        return undefined;
    }

    return {
        processId: constraint.processId,
        processVersion: constraint.processVersion,
        requiredPermissions: Array.from(new Set(
            (constraint.requiredPermissions ?? [])
                .map((permission) => permission.trim())
                .filter((permission) => permission.length > 0),
        ))
            .sort((a, b) => a.localeCompare(b, 'de')),
    };
}

export async function loadDomainAndUserSelectOptions(
    forceReload: boolean = false,
    constraint?: DomainAndUserSelectOptionConstraint,
): Promise<DomainAndUserSelectOption[]> {
    const allOptions = await loadAllDomainAndUserSelectOptions(forceReload);
    const normalizedConstraint = normalizeConstraint(constraint);
    if (normalizedConstraint == null) {
        return allOptions;
    }

    const processAccessControlService = new ProcessInstanceAccessControlApiService();
    const selectableValues = await processAccessControlService.listPotentialSelectableItems(
        normalizedConstraint.processId,
        normalizedConstraint.processVersion,
        normalizedConstraint.requiredPermissions,
    );

    const selectableKeys = new Set(selectableValues
        .map((value) => normalizeDomainAndUserSelectItem(value))
        .filter((value): value is DomainAndUserSelectItem => value != null)
        .map(createDomainAndUserSelectValueKey));

    return allOptions.filter((option) => selectableKeys.has(option.key));
}

async function loadAllDomainAndUserSelectOptions(forceReload: boolean = false): Promise<DomainAndUserSelectOption[]> {
    if (!forceReload && cachedOptions != null) {
        return cachedOptions;
    }

    if (!forceReload && cachedOptionsPromise != null) {
        return cachedOptionsPromise;
    }

    const orgService = new VDepartmentShadowedApiService();
    const teamService = new TeamsApiService();
    const userService = new UsersApiService();

    const promise = Promise
        .all([
            listAllPages((page) => {
                return orgService
                    .list(page, PAGE_SIZE);
            }),
            listAllPages((page) => {
                return teamService
                    .list(page, PAGE_SIZE);
            }),
            listAllPages((page) => {
                return userService
                    .list(page, PAGE_SIZE, undefined, undefined, {
                        deletedInIdp: false,
                        disabledInIdp: false,
                    });
            }),
        ])
        .then(([orgUnits, teams, users]) => {
            const orgOptions: DomainAndUserSelectOption[] = orgUnits.map((orgUnit) => {
                const value = createOrgUnitOptionValue(orgUnit.id);
                return {
                    value,
                    key: createDomainAndUserSelectValueKey(value),
                    label: getDepartmentPath(orgUnit),
                    subLabel: getDepartmentTypeLabel(orgUnit.depth),
                    group: 'Organisationseinheiten',
                    icon: getDepartmentTypeIcons(orgUnit.depth),
                };
            });

            const teamOptions: DomainAndUserSelectOption[] = teams.map((team) => {
                const value = createTeamOptionValue(team.id);
                return {
                    value,
                    key: createDomainAndUserSelectValueKey(value),
                    label: team.name ?? `Team #${team.id}`,
                    subLabel: 'Team',
                    group: 'Teams',
                    icon: ModuleIcons.teams,
                };
            });

            const userOptions: DomainAndUserSelectOption[] = users
                .slice()
                .sort(compareUsers)
                .map((user) => {
                    const value = createUserOptionValue(user.id);
                    return {
                        value,
                        key: createDomainAndUserSelectValueKey(value),
                        label: formatUserDisplayName(user),
                        subLabel: user.email ?? undefined,
                        group: 'Mitarbeitende',
                        icon: createElement(PersonOutlineOutlinedIcon),
                    };
                });

            return sortOptions([
                ...orgOptions,
                ...teamOptions,
                ...userOptions,
            ]);
        })
        .then((options) => {
            cachedOptions = options;
            return options;
        })
        .finally(() => {
            cachedOptionsPromise = undefined;
        });

    cachedOptionsPromise = promise;
    return promise;
}

async function listAllPages<T>(
    fetchPage: (page: number) => Promise<{
        content: T[];
        last: boolean;
    }>,
): Promise<T[]> {
    const items: T[] = [];
    let currentPage = 0;
    let hasMore = true;

    while (hasMore) {
        const response = await fetchPage(currentPage);
        items.push(...response.content);
        hasMore = !response.last;
        currentPage += 1;
    }

    return items;
}
