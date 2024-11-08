//connect to Mongo DB
conn = new Mongo();
db = conn.getDB("payment-wallet");
//create and populate payment wallets collection
db.getCollection('payment-wallets').insertMany([{
  "_id": "00a06383-1495-4b90-88f6-80c5fecf554a",
  "userId": "928bbaed-f7c2-41f9-b47c-9bce088322d6",
  "status": "VALIDATED",
  "paymentMethodId": "148ff003-46a6-4790-9376-b0e057352e45",
  "contractId": "W171896465077471gI",
  "applications": [
    {
      "_id": "PAGOPA",
      "status": "ENABLED",
      "creationDate": "2024-06-21T10:10:50.749146096Z",
      "updateDate": "2024-06-21T10:10:50.749146096Z",
      "metadata": {
        "onboardByMigration": "2024-06-21T10:10:50.749146096Z"
      }
    }
  ],
  "details": {
    "type": "CARDS",
    "bin": "223059",
    "lastFourDigits": "4353",
    "expiryDate": "203204",
    "brand": "MASTERCARD",
    "paymentInstrumentGatewayId": "41795763745a7246744943674e6265776e6a50674c375555754447687534386b4a7574706a5975307030593d",
    "_class": "it.pagopa.wallet.documents.wallets.details.CardDetails"
  },
  "clients": {
    "IO": {
      "status": "ENABLED"
    }
  },
  "version": 2,
  "creationDate": "2024-06-21T10:10:50.802632659Z",
  "updateDate": "2024-06-21T10:34:18.850935581Z",
  "onboardingChannel": "IO",
  "_class": "it.pagopa.wallet.documents.wallets.Wallet"
}]);