const assert = require("node:assert/strict");
const lifecycleFixtures = require("./lifecycle-fixtures.json");

async function main() {
    const mockUrl = "http://localhost:8401";
    const response = await fetch(`${mockUrl}/__test/contract-requests`);
    assert.equal(response.status, 200, "Wallet-service mock request log unavailable");
    const requests = await response.json();

    for (const [scenario, { id, expected }] of Object.entries(lifecycleFixtures)) {
        const count = requests[id] ?? 0;
        if (expected.deactivationRequested) {
            assert.ok(count > 0, `${scenario} (${id}): expected a deactivation call`);
        } else {
            assert.equal(count, 0, `${scenario} (${id}): unexpected deactivation call`);
        }
    }
    console.log("Contract deactivation request checks passed.");
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
