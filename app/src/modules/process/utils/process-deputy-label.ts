import {type ProcessDeputy} from '../entities/process-deputy';

const names = new Intl.ListFormat('de', {style: 'long', type: 'conjunction'});

export function getProcessDeputyLabel(deputies: ProcessDeputy[]): string {
    return `Vertreten durch ${names.format(deputies.map((deputy) => deputy.name))}`;
}
