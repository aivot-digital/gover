import {humanizeNumberCapitalized, pluralize} from './humanization-utils';

const iso8601DurationRegex = /^(-)?P(?:([.,\d]+)Y)?(?:([.,\d]+)M)?(?:([.,\d]+)W)?(?:([.,\d]+)D)?(?:T(?:([.,\d]+)H)?(?:([.,\d]+)M)?(?:([.,\d]+)S)?)?$/;

export function parseISO8601Duration(iso8601Duration: string): {
    sign: '+' | '-';
    years: number;
    months: number;
    weeks: number;
    days: number;
    hours: number;
    minutes: number;
    seconds: number;
} {
    const matches = iso8601Duration.match(iso8601DurationRegex);

    if (!matches || matches.slice(2).every(match => match === undefined)) {
        throw new Error(`${iso8601Duration} is not a valid ISO 8601`);
    }

    return {
        sign: matches[1] === undefined ? '+' : '-',
        years: matches[2] === undefined ? 0 : parseInt(matches[2], 10),
        months: matches[3] === undefined ? 0 : parseInt(matches[3], 10),
        weeks: matches[4] === undefined ? 0 : parseInt(matches[4], 10),
        days: matches[5] === undefined ? 0 : parseInt(matches[5], 10),
        hours: matches[6] === undefined ? 0 : parseInt(matches[6], 10),
        minutes: matches[7] === undefined ? 0 : parseInt(matches[7], 10),
        seconds: matches[8] === undefined ? 0 : parseInt(matches[8], 10),
    };
}

export function humanizeISO8601Duration(iso8602Duration: string): string {
    let {
        years,
        months,
        weeks,
        days,
        hours,
        minutes,
        seconds,
    } = parseISO8601Duration(iso8602Duration);

    return humanizeDuration(years, months, weeks, days, hours, minutes, seconds);
}


export function humanizeMillisecondsDuration(milliseconds: number): string {
    const seconds = Math.floor(milliseconds / 1000) % 60;
    const minutes = Math.floor(milliseconds / (1000 * 60)) % 60;
    const hours = Math.floor(milliseconds / (1000 * 60 * 60)) % 24;
    const days = Math.floor(milliseconds / (1000 * 60 * 60 * 24));

    return humanizeDuration(0, 0, 0, days, hours, minutes, seconds);
}


function humanizeDuration(
    years: number,
    months: number,
    weeks: number,
    days: number,
    hours: number,
    minutes: number,
    seconds: number,
): string {
    const sb: string[] = [];

    if (hours != null && hours > 24) {
        days = Math.floor(hours / 24);
        hours = hours % 24;
    }

    if (years != null && years > 0) {
        sb.push(
            humanizeNumberCapitalized(years, {1: 'Ein'}) +
            ' ' +
            pluralize(years, 'Jahr', 'Jahre'),
        );
    }

    if (months != null && months > 0) {
        sb.push(
            humanizeNumberCapitalized(months, {1: 'Einen'}) +
            ' ' +
            pluralize(months, 'Monat', 'Monate'),
        );
    }

    if (weeks != null && weeks > 0) {
        sb.push(
            humanizeNumberCapitalized(weeks, {1: 'Eine'}) +
            ' ' +
            pluralize(weeks, 'Woche', 'Wochen'),
        );
    }

    if (days != null && days > 0) {
        sb.push(
            humanizeNumberCapitalized(days, {1: 'Einen'}) +
            ' ' +
            pluralize(days, 'Tag', 'Tage'),
        );
    }

    if (hours != null && hours > 0) {
        sb.push(
            humanizeNumberCapitalized(hours, {1: 'Eine'}) +
            ' ' +
            pluralize(hours, 'Stunde', 'Stunden'),
        );
    }

    if (minutes != null && minutes > 0) {
        sb.push(
            humanizeNumberCapitalized(minutes, {1: 'Eine'}) +
            ' ' +
            pluralize(minutes, 'Minute', 'Minute'),
        );
    }

    if (seconds != null && seconds > 0) {
        sb.push(
            humanizeNumberCapitalized(seconds, {1: 'Eine'}) +
            ' ' +
            pluralize(seconds, 'Sekunde', 'Sekunden'),
        );
    }

    if (sb.length >= 2) {
        if (sb.length >= 3) {
            for (let i = 0; i < sb.length - 2; i++) {
                sb[i] = sb[i] + ',';
            }
        }
        sb[sb.length - 1] = ' und ' + sb[sb.length - 1];
    }

    return sb.length > 0 ? sb.join(' ') : '< 1 Sekunde';
}