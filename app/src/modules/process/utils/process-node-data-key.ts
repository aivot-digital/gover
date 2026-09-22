const ALPHABET = '0123456789abcdefghjkmnpqrstvwxyz';
const LETTERS = 'abcdefghjkmnpqrstvwxyz';
const LENGTH = 6;

function randomCharacter(alphabet: string): string {
    const byte = new Uint8Array(1);
    const limit = 256 - (256 % alphabet.length);
    do {
        crypto.getRandomValues(byte);
    } while (byte[0] >= limit);
    return alphabet[byte[0] % alphabet.length];
}

export function generateProcessNodeDataKey(): string {
    let key = randomCharacter(LETTERS);
    for (let index = 1; index < LENGTH; index++) key += randomCharacter(ALPHABET);
    return key;
}
