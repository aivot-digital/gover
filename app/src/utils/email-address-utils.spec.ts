import {describe, expect, it} from 'vitest';
import {normalizeEmailAddress, validateEmailAddress} from './email-address-utils';

// Keep these cases aligned with backend/src/test/resources/email-address-cases.json.
// Inline them so frontend builds do not depend on backend test resources.
const cases = [
  {
    "value": "Person+antrag@EXAMPLE.org",
    "allowLocalDomains": false,
    "normalized": "Person+antrag@example.org"
  },
  {
    "value": " person@example.org ",
    "allowLocalDomains": false,
    "normalized": "person@example.org"
  },
  {
    "value": "\u00a0person@büro.de\u202f",
    "allowLocalDomains": false,
    "normalized": "person@xn--bro-hoa.de"
  },
  {
    "value": "person@verwaltung",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "person@verwaltung",
    "allowLocalDomains": true,
    "normalized": "person@verwaltung"
  },
  {
    "value": "person@localhost",
    "allowLocalDomains": true,
    "normalized": "person@localhost"
  },
  {
    "value": "kontakt@büro.de",
    "allowLocalDomains": false,
    "normalized": "kontakt@xn--bro-hoa.de"
  },
  {
    "value": "kontakt@faß.de",
    "allowLocalDomains": false,
    "normalized": "kontakt@xn--fa-hia.de"
  },
  {
    "value": "müller@example.org",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a..b@example.org",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@example..org",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@-example.org",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@example.org.",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@[127.0.0.1]",
    "allowLocalDomains": true,
    "normalized": null
  },
  {
    "value": "Name <a@example.org>",
    "allowLocalDomains": true,
    "normalized": null
  },
  {
    "value": "a@example.org,b@example.org",
    "allowLocalDomains": true,
    "normalized": null
  },
  {
    "value": "a@example.org\r\nBcc: b@example.org",
    "allowLocalDomains": true,
    "normalized": null
  },
  {
    "value": "a@ex_ample.org",
    "allowLocalDomains": true,
    "normalized": null
  },
  {
    "value": "a@a/b",
    "allowLocalDomains": true,
    "normalized": null
  },
  {
    "value": "a@123",
    "allowLocalDomains": true,
    "normalized": "a@123"
  },
  {
    "value": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa@example.org",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb.de",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "\"a b\"@example.org",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@ab--cd.de",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "a@example.org\r\n",
    "allowLocalDomains": false,
    "normalized": null
  },
  {
    "value": "\u0000a@example.org",
    "allowLocalDomains": false,
    "normalized": null
  }
];

describe('email address contract shared with the backend', () => {
    it.each(cases)('$value (local=$allowLocalDomains)', ({value, allowLocalDomains, normalized}) => {
        expect(normalizeEmailAddress(value, allowLocalDomains) ?? null).toBe(normalized);
        expect(validateEmailAddress(value, allowLocalDomains) == null).toBe(normalized != null);
    });
    it('leaves required validation to the field', () => {
        for (const value of [null, undefined, '', ' ']) expect(validateEmailAddress(value)).toBeUndefined();
    });
});
