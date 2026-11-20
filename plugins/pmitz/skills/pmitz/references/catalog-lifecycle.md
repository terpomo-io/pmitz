# Changing rules and retaining usage

Keep a versioned copy of the intended catalog and load it into each server instance on startup. The default REST product repository is in process memory. The subscription and usage repositories are JDBC-backed. If only the server restarts over the same persistent database, subscriptions and counters survive while product definitions must be uploaded again. A healthy server can therefore have an empty catalog. Verify a known feature through a subscription after bootstrap.

Changing a product through REST requires deleting its ID and posting the full replacement object. A duplicate POST returns 409 and the old rule remains active. The two requests are separate: coordinate the update with application traffic and retain the previous catalog so it can be restored if the upload fails. Deleting a product does not reset its usage. Preserve IDs to continue tracking the same counter.

Five outstanding loans under a new maximum of three give a remaining allowance of **−2**. Resolve it through the application's ordinary returns or wait for the period boundary on a calendar rule. Use a fresh subscription for independent development runs. In 0.10.0, reductions require feature entitlement, so an expired subscription cannot be used for a cleanup reduction.

For embedded Java, the application controls when to load the catalog and how long its database persists.
