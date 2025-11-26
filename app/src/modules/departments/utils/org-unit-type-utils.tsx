import Domain from '@aivot/mui-material-symbols-400-outlined/dist/domain/Domain';
import FiberManualRecord from '@aivot/mui-material-symbols-400-outlined/dist/fiber-manual-record/FiberManualRecord';
import Graph6 from '@aivot/mui-material-symbols-400-outlined/dist/graph-6/Graph6';
import Spoke from '@aivot/mui-material-symbols-400-outlined/dist/spoke/Spoke';
import {decimalNumberToRomanNumeral} from '../../../utils/number-utils';

export function getOrgUnitTypeLabel(depth: number): string {
    switch (depth) {
        case 0:
            return 'Organisation';
        case 1:
            return 'Bereich';
        case 2:
            return 'Abteilung';
        default:
            return `Unterabteilung ${decimalNumberToRomanNumeral(depth - 2)}`;
    }
}

export function getOrgUnitTypeLabelGenitiv(depth: number): string {
    switch (depth) {
        case 0:
            return 'der Organisation';
        case 1:
            return 'des Bereichs';
        case 2:
            return 'der Abteilung';
        default:
            return `der Unterabteilung ${decimalNumberToRomanNumeral(depth - 2)}`;
    }
}

export function getOrgUnitTypeIcons(depth: number) {
    switch (depth) {
        case 0:
            return <Domain />;
        case 1:
            return <Graph6 />;
        case 2:
            return <Spoke />;
        default:
            return <FiberManualRecord />;
    }
}
