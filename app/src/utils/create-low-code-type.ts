export function createLowCodeType(
    name: string,
    properties: Record<string, string>,
    type: 'interface' | 'namespace' = 'interface'
) {
    const lines = [
        `${type} ${name} {`,
    ];

    for (const [key, value] of Object.entries(properties)) {
        lines.push(`    ${type == 'namespace' ? 'const ' : ''}${key}: ${value};`);
    }

    lines.push('}');

    return lines.join('\n');
}