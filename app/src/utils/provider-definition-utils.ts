interface VersionedProviderDefinition {
    key: string;
    version: number;
}

export function getLatestProviderDefinitions<T extends VersionedProviderDefinition>(definitions: readonly T[]): T[] {
    const latestDefinitions = new Map<string, T>();

    definitions.forEach((definition) => {
        const currentLatestDefinition = latestDefinitions.get(definition.key);
        if (currentLatestDefinition == null || definition.version > currentLatestDefinition.version) {
            latestDefinitions.set(definition.key, definition);
        }
    });

    return Array.from(latestDefinitions.values());
}
