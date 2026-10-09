# Process editing through AI chat

The existing `POST /api/ai/chat/send/` endpoint accepts a process context as multipart
fields `processId` and `processVersion`. Supply both together with `chatSessionId`
and `userInput`. Do not send `currentState` or `targetRootType` in this mode.
The process editor opens the chat through its **KI-Chat** action in an additional
resizable column. Sessions and restored histories are separate for each user,
process and version. The existing form chat retains its separate session.

The action requires AI chat permission and read/update access to a drafted process.
It is unavailable in test or instance mode. Before each message, pending node edits
are saved through the normal editor save operation, preserving external-editor
configuration fields. A save failure keeps the message unsent.

During saving, streaming and reloading, manual editing is locked. Every completed
or failed turn reloads the process graph, validation and selected node details from
the database. If the selected node was deleted, the editor returns to the process
overview. A reload failure keeps editing locked and offers **Erneut laden**.
Leaving the page aborts the client stream; it does not roll back committed tools
or guarantee that the server has stopped. Reopening loads the database state.
The **KI-Diagnose herunterladen** action downloads the active session's trace.

The backend authenticates the session owner and checks `ai_chat.use` and
`process_definition.read`. Every mutation additionally checks
`process_definition.update` and requires `Drafted` status. These checks run before
accessing protected data on each tool call. The model cannot choose a user or
process context in its tool arguments. System permission overrides use the existing
`PermissionService` behavior.

## Authoring workflow

1. Read `hole-prozessstruktur`. Search `liste-knotendefinitionen`, then fetch one
   definition with `hole-knotendefinition` for its declared ports and outputs.
2. Create nodes with `erstelle-prozessknoten`. Definitions supply their normal
   initial configuration. Connect node IDs using `speichere-prozessverbindung`.
   An occupied source port requires an explicit update of the existing edge ID.
3. Read `liste-knotenkonfigurationsfelder`, then `hole-knotenkonfigurationsfeld`
   for the needed fields. The backend derives their authoring layout for the
   actual node and requesting user, including visibility and overrides.
4. Resolve selection values through `suche-konfigurationsoptionen`, and variable
   references through `liste-knotenvariablen`. `hole-konfigurationshilfe` provides
   mode examples, No-Code operators and JavaScript declarations on demand.
5. Batch changes with `aktualisiere-prozessknoten`. Finish with `pruefe-prozess`
   and report remaining validation errors.

Example tool arguments (field IDs must come from the actual node layout):

```json
{
  "nodeId": 123,
  "properties": {"name": "Zähler"},
  "configurationChanges": [
    {"valuePath": "/amount", "mode": "Literal", "value": 1},
    {"valuePath": "/description", "mode": "Literal", "value": null}
  ],
  "removePaths": ["/obsoleteValue"]
}
```

`configurationChanges[].valuePath` and entries in `removePaths` are the returned
JSON-pointer paths, not Java configuration property names. `value` is the raw
mode payload: a literal value, a variable reference, a No-Code operand, or
Low-Code JavaScript. Ordinary layout groups do not nest the stored values;
repeating rows do. A row looks like
`{"values":{"fieldId":{"type":"Literal","value":"text"}}}`.
The nested path is `/rows/value/0/values/fieldId`. A `*` in a returned template
path is not writable: create a concrete row first and fetch its actual paths.
A batch may create rows and subsequently configure their children in `configurationChanges` order.

Omitted values remain unchanged. `mode: Literal` with `value: null` persists a null literal;
`removePaths` removes authored entries. Unsupported fields, invalid modes
and incompatible JSON shapes fail the whole call and return path-specific errors.
Incomplete or semantically
invalid configurations can be saved with `savedWithErrors` and validation messages,
as in the existing node editor. Dynamic values are checked in authoring mode,
without evaluating them against runtime process data.

## Embedded forms and persistence

`hole-knotenformular` reads either a paginated structure or one element's
properties. `bearbeite-knotenformular` supports `create`, `update`, `move`, and
`delete` on a literal UI-definition field. Creation generates IDs server-side.
The field's expected root type is enforced. After structural changes, fetch paths
again because array indices may have moved. For large properties, provide
`property` and continue using `valueOffset`.

Every successful mutation commits directly to the database in its own transaction,
with the normal audit event and acting user. There is no Redis process draft and
no separate save/publish tool. Failed calls roll back their own writes, including
checked API failures; earlier successful calls remain committed. Version and affected
row locks protect the read/patch/write cycle. The frontend must reload the process
from the backend after a chat turn, including failed or interrupted turns.

Tool lists default to 20 entries and accept at most 50. Field values and larger
properties use JSON-text previews: continue with the returned `nextOffset` and
concatenate the text chunks before interpreting a complete value. Do not write a
truncated preview back to a node. Validation responses include at most 20 messages
per node and report the remaining count; detailed field checks identify individual
errors. This change does not implement global token counting or history compression.

Option lookups return identifiers and labels, never provider configurations or
secret contents. Supported sources include static/codelist options, teams,
organisation units, users, process-constrained assignments, secrets, storage,
identity/payment providers, assets, data models/objects, process identities and
attachment sets. Other sources return an explicit unsupported result.

Existing execution traces include these tool calls and results. Download an owned
session's trace via `GET /api/ai/chat/trace/?chatSessionId=...`. The existing bounded
unknown-tool recovery remains active; it does not replay already completed calls
when returning an unknown-tool error to the model.
