export type Brand<T, TBrand extends string> = T & {
    readonly __brand: TBrand;
};

export type InstantIso = Brand<string, 'InstantIso'>;
export type LocalDateIso = Brand<string, 'LocalDateIso'>;
export type YearMonthIso = Brand<string, 'YearMonthIso'>;
export type YearIso = Brand<string, 'YearIso'>;
export type DateValueIso = LocalDateIso | YearMonthIso | YearIso;
export type LocalTimeIso = Brand<string, 'LocalTimeIso'>;
export type LocalDateTimeIso = Brand<string, 'LocalDateTimeIso'>;
export type IanaTimeZone = Brand<string, 'IanaTimeZone'>;

export interface ZonedDateTimeValue {
    localDateTime: LocalDateTimeIso;
    timeZone: IanaTimeZone;
}

export interface TemporalRange<T> {
    start: T | null;
    end: T | null;
}
