/*
    This script is intended to be executed in the mongosh of the mongo container during
    the integration test executed in the code-review-pipelines.
*/
const dbName = "payment-wallet";
db = db.getSiblingDB(dbName);
const coll = db.getCollection("payment-wallets");
const docs = coll.find().toArray();

let longTermAllowedStatuses = ["DELETED", "REPLACED"]
let shortTermAllowedStatuses = ["CREATED", "INITIALIZED", "VALIDATION_REQUESTED", "ERROR"]
let npgValidOperationResult = ["EXECUTED"]

let hasError = false;

function assertTtl(doc) {
   const status = doc.status;

   // TODO: Assicurati che i nomi dei campi Date siano corretti per il tuo modello
   const updateDate = doc.updateDate.getUTCDate();
   const ttl = doc.ttl;

   // Check in which case the doc must be
   const isLongTermStatus = status in longTermAllowedStatuses;
   const isShortTermStatus = status in shortTermAllowedStatuses;
   const isNpgValidOperationResult = doc.validationOperationResult in npgValidOperationResult;

   // Not handled case, ttl == -1, null or undefined
   if (!isLongTermStatus && !isShortTermStatus && !isNpgValidOperationResult) {
        if(ttl || ttl !== null || ttl !== -1){
            hasError = true;
            return;
        }
   }

   // Check if the ttl is setted
   if(!ttl){
       print(`ERRORE: Doc ${doc._id}. TTL atteso con un valore finito ma trovato ~${ttl}`);
       hasError = true;
       return;
   }

   // Calculate the expired date
   let expectedExpiredDate = updateDate.copy();
   if(isLongTermStatus || isNpgValidOperationResult){
        // Add 10 years
        expectedExpiredDate.setFullYear(expiredDate.getFullYear() + 10);
   }else if(isShortTermStatus){
        // Add 90 days
        expectedExpiredDate.setUTCDate(expiredDate.getUTCDate() + 90);
   }
   let expiredDate = updateDate.copy()
   expiredDate = expiredDate.setUTCSeconds(expiredDate.getUTCSeconds() + ttl);

   // Check if the document updateDate is too old -> instantaneus delete 5 seconds
   const limitDate = new Date();
   limitDate.setFullYear(limitDate.getFullYear() - 10);

   if(limitDate > expiredDate && ttl !== 5){
        print(`ERRORE: Doc ${doc._id} with updateDate < 10 years. TTL atteso 5, trovato ~${ttl} s`);
        hasError = true;
        return;
    }

    if(!(expiredDate.getUTCFullYear() === expectedExpiredDate.getUTCFullYear() &&
           expiredDate.getUTCMonth()    === expectedExpiredDate.getUTCMonth() &&
           expiredDate.getUTCDate()     === expectedExpiredDate.getUTCDate())){
            hasError = true;
            return;
    }

}

docs.forEach(doc => {
   assertTtl(doc);
});

if (hasError) {
   print("Verifica TTL FALLITA!");
   quit(1);
} else {
   print("Verifica TTL completata con SUCCESSO!");
}

