module.exports = function (req, res, next) {
  console.log(`${new Date().toISOString()} - Received request: ${req.method} ${req.path}
     Headers:
     ${JSON.stringify(req.headers)}
     Body:
     ${JSON.stringify(req.body)}
     `);

  if (req.path.includes("contract") && req.method === 'DELETE') {
    return res.status(204).send();
  }

  next();
};
