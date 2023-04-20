import {DateFieldElement, DateFieldComponentModelMode} from '../../models/elements/./form/./input/date-field-element';
import {BaseInputElementValidator} from '../../validators/base-input-element-validator';
import {isFuture, isPast, isToday} from 'date-fns';

export class DateFieldComponentValidator extends BaseInputElementValidator<string, DateFieldElement> {
    protected checkEmpty(comp: DateFieldElement, value: string): boolean {
        return value.length === 0;
    }

    protected getEmptyErrorText(comp: DateFieldElement): string {
        return 'Dies ist eine Pflichtangabe. Bitte füllen Sie das Feld entsprechend aus.';
    }

    protected makeSpecificErrors(comp: DateFieldElement, value: string | undefined, userInput: any): string | null {
        /*TODO: case currently not reachable, error message should include date format of input*/
        if (value != null) {
            const timestamp = Date.parse(value);
            if (isNaN(timestamp)) {
                return 'Bitte geben Sie ein Datum im geforderten Format ein.'
            }

            const date = new Date(value);
            if ((comp.mode == null || comp.mode === DateFieldComponentModelMode.Date) && comp.mustBeFuture) {
                if (!isFuture(date)) {
                    return  'Bitte geben Sie ein Datum ein, das in der Zukunft liegt.';
                }
            } else if ((comp.mode == null || comp.mode === DateFieldComponentModelMode.Date) && comp.mustBePast) {
                if (isToday(date) || !isPast(date)) {
                    return 'Bitte geben Sie ein Datum ein, das in der Vergangenheit liegt.';
                }
            }
        }

        return null;
    }
}
