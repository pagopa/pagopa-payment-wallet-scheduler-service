// Run with mongosh against the Docker Compose database after the lifecycle job starts.
const walletDb = db.getSiblingDB("payment-wallet");
const wallets = walletDb.getCollection("payment-wallets");
const dlq = walletDb.getCollection("wallet-contractId-deactivation-dlq");
const lifecycleFixtures = require("/fixtures/lifecycle-fixtures.json");

function checkFixtures() {
    const errors = [];
    for (const [scenario, { id, expected }] of Object.entries(lifecycleFixtures)) {
        const label = `${scenario} (${id})`;
        const wallet = wallets.findOne({ _id: id });
        if (!wallet) {
            errors.push(`${label}: wallet not found`);
            continue;
        }
        if ((wallet.ttl ?? null) !== expected.ttl) {
            errors.push(`${label}: expected TTL ${expected.ttl}, got ${wallet.ttl}`);
        }
        if (expected.deactivationRetryCount !== undefined &&
            wallet.deactivationRetryCount !== expected.deactivationRetryCount) {
            errors.push(`${label}: expected retry count ${expected.deactivationRetryCount}, got ${wallet.deactivationRetryCount}`);
        }
        const entry = dlq.findOne({ _id: id });
        if (expected.dlq) {
            if (!entry || typeof entry.timestamp !== "string" || Number.isNaN(Date.parse(entry.timestamp))) {
                errors.push(`${label}: missing DLQ entry or invalid timestamp`);
            }
        } else if (entry) {
            errors.push(`${label}: unexpected DLQ entry`);
        }
    }
    return errors;
}

const deadline = Date.now() + 90000;
let errors;
do {
    errors = checkFixtures();
    if (errors.length === 0) {
        print("Lifecycle wallet and DLQ checks passed.");
        quit(0);
    }
    if (Date.now() < deadline) sleep(1000);
} while (Date.now() < deadline);

errors.forEach(error => print(`ERROR: ${error}`));
quit(1);
