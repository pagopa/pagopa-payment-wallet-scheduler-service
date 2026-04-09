/*
    This script is intended to be executed in the mongosh of the mongo container during
    the integration test executed in the code-review-pipelines.
*/
console.log("Starting integration test...")
const dbName = "payment-wallet";
db = db.getSiblingDB(dbName);
const coll = db.getCollection("payment-wallets");
const docs = coll.find().toArray();

let longTermAllowedStatuses = ["DELETED", "REPLACED"]
let shortTermAllowedStatuses = ["CREATED", "INITIALIZED", "VALIDATION_REQUESTED", "ERROR"]
let npgValidOperationResult = ["EXECUTED"]


function assertTtl(doc) {
   const status = doc.status;
   const updateDate = new Date(doc.updateDate);
   const ttl = doc.ttl;

   // Check in which case the doc must be
   const isLongTermStatus = status in longTermAllowedStatuses;
   const isShortTermStatus = status in shortTermAllowedStatuses;
   const isNpgValidOperationResult = doc.validationOperationResult in npgValidOperationResult;

   // Not handled case, ttl == -1, null or undefined
   if (!isLongTermStatus && !isShortTermStatus && !isNpgValidOperationResult) {
        if(!ttl || ttl === null || ttl === -1){
            return true;
        }
   }

   // Check if the ttl exist
   if(!ttl){
       print(`ERROR: Doc ${doc._id}. TTL expected with a defined value but: ~${ttl}`);
       return false;
   }

   // Calculate the expired date and expected expired date
   let expectedExpiredDate = new Date(updateDate);
   let expiredDate = new Date(updateDate);
   expiredDate = expiredDate.setUTCSeconds(expiredDate.getUTCSeconds() + ttl);
   print(`log: expectedExpiredDate ${expectedExpiredDate.toUTCString()}, expiredDate ${expiredDate.toString()}.`);
   let limitDate = new Date();

   if(isLongTermStatus || isNpgValidOperationResult){
        // Add 10 years
        expectedExpiredDate.setFullYear(expiredDate.getFullYear() + 10);
        limitDate.setFullYear(limitDate.getFullYear() - 10);
   }else if(isShortTermStatus){
        // Add 90 days
        expectedExpiredDate.setUTCDate(expiredDate.getUTCDate() + 90);
        limitDate.setUTCDate(limitDate.getUTCDate() - 90);
   }


   // Check if the document updateDate is too old -> instantaneus delete 5 seconds
   if(limitDate > expiredDate && ttl !== 5){
        print(`ERROR: Doc ${doc._id} with updateDate < limiteDate: ${limitDate.toUTCString()}. TTL expected 5, ttl: ~${ttl} s`);
        return false;
    }

    // Check if the expected date and the expired date are comparable
    return (expiredDate.getUTCFullYear() === expectedExpiredDate.getUTCFullYear() &&
           expiredDate.getUTCMonth()    === expectedExpiredDate.getUTCMonth() &&
           expiredDate.getUTCDate()     === expectedExpiredDate.getUTCDate())

}


let hasError = false;

docs.forEach(doc => {
   if(assertTtl(doc)){
        print(`Verification of doc ${doc._id} completed successfully`);
   }else{
        print(`ERROR: Doc ${doc._id}`);
        hasError = true;
    };
});


if (hasError) {
   print("Integration test failed!");
   quit(1);
} else {
   print("Integration test completed SUCCESSFULLY!");
}
