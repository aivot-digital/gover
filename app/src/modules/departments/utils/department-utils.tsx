import CorporateFare from '@aivot/mui-material-symbols-400-outlined/dist/corporate-fare/CorporateFare';
import FiberManualRecord from '@aivot/mui-material-symbols-400-outlined/dist/fiber-manual-record/FiberManualRecord';
import Label from '@aivot/mui-material-symbols-400-outlined/dist/label/Label';
import Spoke from '@aivot/mui-material-symbols-400-outlined/dist/spoke/Spoke';
import {decimalNumberToRomanNumeral} from '../../../utils/number-utils';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {VDepartmentShadowedEntity} from '../entities/v-department-shadowed-entity';
import Graph6 from '@aivot/mui-material-symbols-400-outlined/dist/graph-6/Graph6';

const DefaultDepartmentTypeLabels = [
    'Organisation',
    'Bereich',
    'Abteilung',
    'Sachgebiet',
    'Arbeitsbereich',
];

function getDefaultDepartmentTypeLabel(depth: number): string {
    return DefaultDepartmentTypeLabels[depth] ?? `Unterebene ${decimalNumberToRomanNumeral(depth - DefaultDepartmentTypeLabels.length + 1)}`;
}

export function getDepartmentTypeLabels(): string[] {
    const configuredLabels = typeof AppConfig !== 'undefined' && Array.isArray(AppConfig.departmentLevelLabels)
        ? AppConfig.departmentLevelLabels
            .map((label) => label.trim())
        : [];

    return configuredLabels.some(isStringNotNullOrEmpty)
        ? configuredLabels.map((label, index) => isStringNotNullOrEmpty(label) ? label : getDefaultDepartmentTypeLabel(index))
        : DefaultDepartmentTypeLabels;
}

export function getMaxDepartmentDepth(): number {
    return getDepartmentTypeLabels().length - 1;
}

export function getDepartmentTypeLabel(depth: number): string {
    return getDepartmentTypeLabels()[depth] ?? getDefaultDepartmentTypeLabel(depth);
}

export function getDepartmentTypeIcons(depth: number) {
    switch (depth) {
        case 0:
            return <CorporateFare />;
        case 1:
            return <Graph6 />;
        case 2:
            return <Spoke />;
        case 3:
            return <FiberManualRecord />;
        default:
            return <Label />;
    }
}

export function getDepartmentPath(org: VDepartmentShadowedEntity): string {
    if (org.parentNames == null || org.parentNames.length === 0) {
        return org.name;
    }
    return org.parentNames.join(' › ') + ' › ' + org.name;
}

export function getDepartmentDisplayAddress(org?: { postalAddress?: string | null } | null): string | undefined {
    if (org == null || !isStringNotNullOrEmpty(org.postalAddress)) {
        return undefined;
    }

    return org.postalAddress ?? undefined;
}
