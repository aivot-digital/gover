---
name: prosuna-ui-copy
description: Write and review German product copy for Prosuna UI controls, dialogs, validation and user-visible errors, and product emails. Apply Prosuna terminology, correct umlauts, and gender-inclusive language. Use when creating or changing these texts, not for unrelated code changes, commit messages, or technical documentation.
---

# Prosuna UI Copy

Write clear, respectful German for people submitting information and for staff handling administrative processes. Preserve the meaning of the operation, its permission scope, and its consequences.

## Establish the Context

- Read the affected screen or template and the behavior behind the message. Check whether it serves staff, customers, or both; inspect related validation or backend handling only as needed to establish the facts.
- Preserve configured customer wording and forms of address. Do not rewrite customer-authored process content, organization names, quoted or legally prescribed text as part of an ordinary copy change.
- Keep the requested scope. A wording task does not authorize new UI features, a terminology migration, changes to authorization, or a repository-wide copy cleanup.

## German and Inclusive Language

- Use correct German spelling, capitalization, and actual UTF-8 umlauts and `ß`: `Änderungen`, `löschen`, `prüfen`, `Schließen`, not ASCII transliterations. Do not mechanically replace valid `ss` spellings or transliterate technical identifiers.
- Address people as `Sie` with `Ihr`/`Ihre`, unless the affected customer experience deliberately uses another form. Prefer direct instructions such as `Prüfen Sie Ihre Eingaben.` over impersonal administrative phrasing.
- Prefer neutral wording when it preserves the exact meaning: `Personen`, `Teammitglieder`, or direct address. Do not substitute `Teammitglieder` for all staff, or `Antragstellende` for people who have not submitted an application.
- Where a neutral expression is awkward or would change the meaning, use the established colon style, for example `Mitarbeiter:innen`. Prefer direct address or plural phrasing over difficult singular article and pronoun combinations. Do not alternate between colon, asterisk, slash, and generic masculine forms for the same group.
- Keep a calm, factual tone suitable for public administration. Be approachable without jokes, praise, marketing claims, or blame in errors. Explain specialist terms when necessary, but do not erase meaningful domain distinctions.

## Product Terminology

Use the affected module's established terms consistently across labels, messages, and accessible names. These distinctions are especially important:

| Term | Meaning to preserve |
| --- | --- |
| `Prosuna` | Product name; legacy `Gover` code identifiers are not a copy-cleanup target. |
| `Prozess` | The modeled administrative workflow, distinct from an individual execution. |
| `Vorgang` | A concrete process instance; do not rename it to `Antrag` unless it actually represents an application. |
| `Organisationseinheit` | The department concept; not interchangeable with `Team`. |
| `Systemrolle` | A role granting permissions globally, not a synonym for an administrator. |
| `Domänenrolle` | A scoped role assigned through memberships; legacy `UserRole` identifiers do not imply a separate user-role concept. |
| `Berechtigung` | An allowed capability; distinguish the permission from the role that grants it. |

Treat this as a terminology guide, not permission to rename API fields, routes, database objects, permission keys, or customer-authored labels. Consult the root `AGENTS.md` for authorization rules.

## Writing by UI State

- **Labels and actions:** Use concise German labels and familiar object-plus-infinitive actions such as `Domänenrolle speichern`. Use `Speichern` when the object is already unambiguous. Keep necessary qualifiers; do not apply English verb-first word order or arbitrary character limits.
- **Validation:** Name the field or input that needs correction and describe the actual rule. Do not invent length limits, accepted formats, or required fields; derive them from validation. Keep examples clearly distinct from entered values.
- **Errors:** State the known problem and a useful next step only when supported. Do not guess a cause, expose internals, or claim insufficient permissions for an unknown failure. If the result of a mutation is uncertain, do not promise that nothing was saved. Recommend a retry only when meaningful and safe for that operation.
- **Empty and loading states:** Distinguish no records, no filter matches, missing access, loading, and failed loading. Do not offer an action the person cannot perform or disclose protected resource details through a message.
- **Confirmations and success:** Name the affected object and action. State destructive effects, reassignment, or irreversibility only when true. Match the confirmation button to the action, and show success only after the relevant operation succeeds. Distinguish saving a definition from starting or completing a process instance.
- **Accessibility and layout:** Keep visible labels, hints, errors, and accessible names consistent with shared form-field semantics. Reuse existing pluralization and locale-formatting helpers. Check zero, one, many, and long inserted names where relevant. Follow the root viewport rules; do not remove meaningful wording just to fit a narrow control.

## Examples

These are patterns to adapt to the actual behavior, not replacement rules for every occurrence.

| Context | Avoid | Prefer |
| --- | --- | --- |
| German action label | `Aenderungen uebernehmen` | `Änderungen übernehmen` |
| Instruction addressed to the person | `Jeder Benutzer muss seine Eingaben prüfen.` | `Prüfen Sie Ihre Eingaben.` |
| Staff assignments heading | `Zugeordnete Mitarbeiter` | `Zugeordnete Mitarbeiter:innen` |
| Missing required role name | `Ungültige Eingabe` | `Geben Sie einen Namen für die Domänenrolle ein.` |
| Delete-role dialog | Title `Sind Sie sicher?`, button `OK` | Title `Domänenrolle löschen?`, buttons `Domänenrolle löschen` and `Abbrechen`; explain the verified consequences in the body. |
| Known permission denial for an update | `Zugriff verweigert` | `Sie haben keine Berechtigung, dieses Team zu bearbeiten.` |
| Search with no matches | `Keine Organisationseinheiten vorhanden` | `Keine Organisationseinheiten für den Suchbegriff gefunden.` |

## Verify the Change

- Re-read the changed text in its actual state: is the object, action, permission scope, and outcome correct? Check grammar after interpolation, umlauts, inclusive wording, and consistency with related messages.
- Preserve placeholders, escaping, links, and markup. Do not alter machine-readable values or generated files as copy sources.
- If production text changes, update affected behavioral or accessible-name assertions without weakening them. Use the smallest relevant checks from `AGENTS.md`; do not start development services or demand screenshots for a wording-only change.
