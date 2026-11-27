import {stringToPastelColor} from '../../../../components/avatar/string-avatar';
import {
    type OrganizationChartDepartmentItem,
    type OrganizationChartTeamItem,
    type OrganizationChartUserItem,
} from './organization-chart-types';
import {
    compareNamedItems,
    compareOrganizationChartUsers,
    sortOrganizationChartDepartmentTrees,
} from './organization-chart-utils';

interface SimulatedOrganizationChartData {
    rootDepartments: OrganizationChartDepartmentItem[];
    teams: OrganizationChartTeamItem[];
}

interface SimulatedOrganizationChartOptions {
    canReadDepartmentMemberships?: boolean;
    canReadTeamMemberships?: boolean;
}

const SIMULATED_CREATED = '2026-01-01T00:00:00.000Z';
const SIMULATED_DEPARTMENT_MEMBER_COUNTS = [
    0,
    1,
    4,
    5,
    12,
    2,
    18,
    3,
    7,
    24,
    4,
    9,
    16,
    0,
    6,
    31,
    2,
    11,
    5,
    20,
    1,
    14,
    4,
    8,
    27,
];
const SIMULATED_TEAM_MEMBER_COUNTS = [0, 1, 4, 5, 9, 2, 14, 6, 18, 3, 11];
const SIMULATED_FIRST_NAMES = [
    'Aylin',
    'Ben',
    'Clara',
    'Daria',
    'Emil',
    'Fatima',
    'Goran',
    'Helena',
    'Ida',
    'Jonas',
    'Kira',
    'Lea',
    'Milan',
    'Nora',
    'Oskar',
    'Paula',
    'Quang',
    'Rena',
    'Samir',
    'Tessa',
    'Udo',
    'Vera',
    'Wim',
    'Yara',
];
const SIMULATED_LAST_NAMES = [
    'Agave',
    'Blaubeere',
    'Cembalo',
    'Drossel',
    'Efeu',
    'Fichte',
    'Granat',
    'Holunder',
    'Iris',
    'Jasmin',
    'Kastanie',
    'Litschi',
    'Mandel',
    'Nektar',
    'Oleander',
    'Pappel',
    'Quelle',
    'Rhabarber',
    'Sonne',
    'Tanne',
    'Ulme',
    'Veilchen',
    'Weide',
    'Zeder',
];

export function createSimulatedOrganizationChartData(options: SimulatedOrganizationChartOptions = {}): SimulatedOrganizationChartData {
    const rootDepartments = [
        createSimulatedCityAdministrationOrganisation(),
        createSimulatedMunicipalServicesOrganisation(),
        createSimulatedCultureAndEducationOrganisation(),
    ];
    const departments = flattenDepartments(rootDepartments);

    departments.forEach((department, index) => {
        department.canReadMemberships = options.canReadDepartmentMemberships ?? true;
        department.members = createSimulatedMembers(
            SIMULATED_DEPARTMENT_MEMBER_COUNTS[index % SIMULATED_DEPARTMENT_MEMBER_COUNTS.length],
            `department-${department.id}`,
            index,
        );
    });

    const teams = createSimulatedTeams();
    teams.forEach((team, index) => {
        team.canReadMemberships = options.canReadTeamMemberships ?? true;
        team.members = createSimulatedMembers(
            SIMULATED_TEAM_MEMBER_COUNTS[index % SIMULATED_TEAM_MEMBER_COUNTS.length],
            `team-${team.id}`,
            index + departments.length,
        );
    });

    sortOrganizationChartDepartmentTrees(rootDepartments);
    teams.sort(compareNamedItems);

    return {
        rootDepartments,
        teams,
    };
}

function createSimulatedCityAdministrationOrganisation(): OrganizationChartDepartmentItem {
    const root = createSimulatedDepartment(-8000, 'Stadtverwaltung Mitte', 0, null, []);
    const citizenOffice = createSimulatedDepartment(-8001, 'FB 1 - Bürgerservice', 1, root.id, [root.id]);
    const environment = createSimulatedDepartment(-8002, 'FB 2 - Umwelt und Bauen', 1, root.id, [root.id]);
    const orderAndSocial = createSimulatedDepartment(-8003, 'FB 3 - Ordnung und Soziales', 1, root.id, [root.id]);
    const registration = createSimulatedDepartment(-8004, 'Meldewesen', 2, citizenOffice.id, [root.id, citizenOffice.id]);
    const permits = createSimulatedDepartment(-8005, 'Genehmigungen', 2, environment.id, [root.id, environment.id]);
    const socialPlanning = createSimulatedDepartment(-8006, 'Sozialplanung', 2, orderAndSocial.id, [root.id, orderAndSocial.id]);
    const citizenAppointments = createSimulatedDepartment(-8007, 'Bürgertermine', 3, registration.id, [root.id, citizenOffice.id, registration.id]);
    const appointmentDesk = createSimulatedDepartment(-8008, 'Terminservice vor Ort', 4, citizenAppointments.id, [root.id, citizenOffice.id, registration.id, citizenAppointments.id]);

    citizenAppointments.children.push(appointmentDesk);
    registration.children.push(citizenAppointments);
    citizenOffice.children.push(registration);
    environment.children.push(permits);
    orderAndSocial.children.push(socialPlanning);
    root.children.push(citizenOffice, environment, orderAndSocial);

    return root;
}

function createSimulatedMunicipalServicesOrganisation(): OrganizationChartDepartmentItem {
    const root = createSimulatedDepartment(-9000, 'Kommunalservice Nord', 0, null, []);
    const service = createSimulatedDepartment(-9001, 'Service und Betrieb', 1, root.id, [root.id]);
    const projects = createSimulatedDepartment(-9002, 'Projektkoordination', 1, root.id, [root.id]);
    const citizenServices = createSimulatedDepartment(-9003, 'Bürgernahe Dienste', 2, service.id, [root.id, service.id]);

    service.children.push(citizenServices);
    root.children.push(service, projects);

    return root;
}

function createSimulatedCultureAndEducationOrganisation(): OrganizationChartDepartmentItem {
    const root = createSimulatedDepartment(-9100, 'Kultur und Bildung', 0, null, []);
    const schools = createSimulatedDepartment(-9101, 'Schulen', 1, root.id, [root.id]);
    const libraries = createSimulatedDepartment(-9102, 'Bibliotheken', 1, root.id, [root.id]);
    const events = createSimulatedDepartment(-9103, 'Veranstaltungen', 1, root.id, [root.id]);
    const schoolDevelopment = createSimulatedDepartment(-9104, 'Schulentwicklung', 2, schools.id, [root.id, schools.id]);

    schools.children.push(schoolDevelopment);
    root.children.push(schools, libraries, events);

    return root;
}

function createSimulatedTeams(): OrganizationChartTeamItem[] {
    return [
        createSimulatedTeam(-7000, 'Digitalisierung'),
        createSimulatedTeam(-7001, 'Krisenstab'),
        createSimulatedTeam(-7002, 'Bauprojekt Innenstadt'),
        createSimulatedTeam(-7003, 'Serviceportal'),
        createSimulatedTeam(-7004, 'Veranstaltungskoordination'),
        createSimulatedTeam(-7005, 'Datenschutz'),
        createSimulatedTeam(-7006, 'Klimabeirat'),
        createSimulatedTeam(-7007, 'Schulentwicklung'),
        createSimulatedTeam(-7008, 'Onboarding'),
        createSimulatedTeam(-7009, 'Haushaltsplanung'),
        createSimulatedTeam(-7010, 'Notfallkommunikation'),
    ];
}

function createSimulatedDepartment(
    id: number,
    name: string,
    depth: number,
    parentDepartmentId: number | null,
    parentIds: number[],
): OrganizationChartDepartmentItem {
    return {
        id,
        name,
        created: SIMULATED_CREATED,
        updated: SIMULATED_CREATED,
        depth,
        parentDepartmentId,
        parentIds,
        parentNames: null,
        color: stringToPastelColor(name),
        children: [],
        canReadDetails: true,
        canReadMemberships: true,
        members: [],
    };
}

function createSimulatedTeam(id: number, name: string): OrganizationChartTeamItem {
    return {
        id,
        name,
        created: SIMULATED_CREATED,
        updated: SIMULATED_CREATED,
        color: stringToPastelColor(name),
        canReadDetails: true,
        canReadMemberships: true,
        members: [],
    };
}

function flattenDepartments(departments: OrganizationChartDepartmentItem[]): OrganizationChartDepartmentItem[] {
    return departments.flatMap((department) => [
        department,
        ...flattenDepartments(department.children),
    ]);
}

function createSimulatedMembers(
    count: number,
    scopeKey: string,
    scopeIndex: number,
): OrganizationChartUserItem[] {
    return Array.from({length: count}, (_, memberIndex) => {
        const firstName = SIMULATED_FIRST_NAMES[(scopeIndex + memberIndex) % SIMULATED_FIRST_NAMES.length];
        const lastName = SIMULATED_LAST_NAMES[((scopeIndex * 3) + memberIndex) % SIMULATED_LAST_NAMES.length];
        const fullName = `${firstName} ${lastName}`;
        const enabled = memberIndex % 9 !== 8;

        return {
            id: `simulated-${scopeKey}-${memberIndex}`,
            email: `${firstName}.${lastName}.${scopeKey}@example.invalid`.toLowerCase(),
            firstName,
            lastName,
            fullName,
            enabled,
            verified: true,
            deletedInIdp: false,
            systemRoleId: null,
        };
    }).sort(compareOrganizationChartUsers);
}
