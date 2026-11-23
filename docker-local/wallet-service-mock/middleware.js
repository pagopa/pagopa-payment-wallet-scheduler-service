const lifecycleFixtures = require('/fixtures/lifecycle-fixtures.json');
const contractPath = /^\/wallets\/([^/]+)\/contract\/?$/;
const fixturesById = new Map(Object.values(lifecycleFixtures).map(fixture => [fixture.id, fixture]));
const contractRequests = new Map();

module.exports = function (req, res, next) {
  if (req.path === '/__test/contract-requests' && req.method === 'GET') {
    return res.json(Object.fromEntries(contractRequests));
  }
  console.log(`${new Date().toISOString()} - Received request: ${req.method} ${req.path}
     Headers: ${JSON.stringify(req.headers)}
     Body:    ${JSON.stringify(req.body)}
     `);

  const contract = contractPath.exec(req.path);
  if (contract && req.method === 'DELETE') {
    const id = contract[1];
    const fixture = fixturesById.get(id);
    if (fixture) {
      contractRequests.set(id, (contractRequests.get(id) ?? 0) + 1);
    }
    return res.status(fixture?.mockStatus ?? 204).send();
  }

  next();
};
