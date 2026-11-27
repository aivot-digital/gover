/** Match the backend publication scan without interpreting customer-authored strings as rule definitions. */
export function findLegacyNoCode(value: unknown, path = 'Konfiguration'): string[] {
    if (Array.isArray(value)) return value.flatMap((child, i) => findLegacyNoCode(child, `${path}[${i}]`));
    if (value == null || typeof value !== 'object') return [];
    const data = value as Record<string, unknown>;
    const found: string[] = [];
    if (typeof data.id === 'string' && typeof data.type === 'number') {
        for (const section of ['visibility', 'validation']) {
            const fn = data[section] as { conditionSet?: unknown } | undefined;
            if (fn?.conditionSet != null && typeof fn.conditionSet === 'object') found.push(`${data.label ?? data.id} · ${section === 'visibility' ? 'Sichtbarkeit' : 'Validierung'} · ${path}.${section}`);
        }
    }
    if (data.type === 'ConditionSet' && data.conditionSet != null && typeof data.conditionSet === 'object' && !/\.(visibility|validation)$/.test(path)) found.push(path);
    return [...found, ...Object.entries(data).flatMap(([key, child]) => findLegacyNoCode(child, `${path}.${key}`))];
}
