# FIT-Connect ZBP delivery verification

## Upstream release prerequisite

Prosuna currently uses FIT-Connect Java SDK `4.0.0-rc.1`. Its
`ApiAttachment` constructor normalizes absent fragments to an empty list;
the serializer includes that list as `fragments: []`. Metadata schemas 1.3.0,
2.0.0 and 3.0.0 require at least one fragment when the property is present.

The upstream fix must omit empty `fragments` during serialization while
retaining real fragment IDs. Add upstream regression coverage for both
`PreparedAttachment.withoutFragments` and pre-encrypted attachments, as well as
fragmented attachments. Do not downgrade metadata schemas or force chunking.
No corrected release was available when this change was implemented.

`FitConnectAttachmentMetadataCompatibilityTest` uses the SDK's actual metadata
builder and serializer. Its fixture is the unmodified public schema from
<https://schema.fitko.de/fit-connect/metadata/2.0.0/metadata.schema.json>.
The unfragmented-attachment acceptance case is reported as skipped while the
known SDK defect remains. Make this a mandatory release check with:

```sh
cd backend
mvn --batch-mode -Dtest=FitConnectAttachmentMetadataCompatibilityTest -Dfitconnect.requireFixedSdk=true test
```

Once a corrected SDK is published, update `fitconnect.version`, remove the
conditional skip, and run this check alongside the ZBP provider tests and the
other FIT-Connect integration tests. This repository does not contain or patch
the upstream SDK source.

The supplied reject event for submission
`d3469af1-7fc3-49ae-ab77-8b8ff0fa1c50` identifies `metadata` as the invalid
instance. Confirm its actual `$schema` and serialized metadata before attributing
that historical rejection solely to the empty-fragments defect.

## Delivery behavior

Submission receipts and a versioned process-result snapshot survive application
restarts. A submission is not a delivery confirmation. The task waits for the
adapter's ACCEPTED/REJECTED event; acceptance does not mean the citizen has read
the message. Completed historical sends are not reclassified or resent.

The monitor polls every 10 seconds initially, every minute after five minutes,
and backs off to five minutes after repeated query failures. A warning after
24 hours does not turn an unknown result into a rejection. An interrupted send
without a receipt requires manual investigation in FIT-Connect; it is never
automatically resent. Temporary test delivery records expire after seven days.
Process delivery records follow the process/task deletion lifecycle.
After acceptance, payment requests recheck the existing transaction to reconcile
callbacks received while the notification was pending. No new transaction is
created.

## Integration acceptance

Using the developer-managed test environment, send a message with a PDF and
verify the adapter's ACCEPTED event and downloadable attachment. Check the
configured operator name, department sender, visible process case number,
paragraphs, single line breaks, umlauts and action links in BundID. Also verify
a rejected submission, a temporarily unavailable status API, and a restart
while a task waits. The process must continue once without another message or
payment transaction. Check staff views at 1280 × 720 and a larger desktop size.

Both ordered example datasets (`bad-musterstadt` and `vg-bergwald`) contain no
communication delivery rows. The new table has no required seed data; their
existing insert order remains valid.
