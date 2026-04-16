/*
    This script is intended to be executed in the mongosh of the mongo container during
    the integration test executed in the code-review-pipelines.
    To test it in local use this bash script:

    #!/bin/bash
    #sleep 15
    echo "Starting wallet lifecycle management integration testing..."

    docker cp lifecycle-integration-test.js mongodb:/tmp/lifecycle-integration-test.js

    # Executing the script and check the result
    docker exec mongodb mongosh -u admin -p password --authenticationDatabase admin /tmp/lifecycle-integration-test.js

    # Check of the exit code of the script
    if [ $? -ne 0 ]; then
      echo "Integration test of lifecycle management failed! Check the logs for more details ..."
      exit 1
    fi
*/
console.log("Starting integration test...")
const dbName = "payment-wallet";
db = db.getSiblingDB(dbName);
const coll = db.getCollection("payment-wallets");
const docs = coll.find().toArray();

const longTermAllowedStatuses = ["DELETED", "REPLACED"]
const shortTermAllowedStatuses = ["CREATED", "INITIALIZED", "VALIDATION_REQUESTED", "ERROR"]
const npgValidOperationResult = ["EXECUTED"]


function assertTtl(doc) {
   const status = doc.status;
   const updateDate = new Date(doc.updateDate);
   const ttl = doc.ttl;

   // Check in which case the doc must be
   const isLongTermStatus = longTermAllowedStatuses.includes(status);
   const isShortTermStatus = shortTermAllowedStatuses.includes(status);
   const isNpgValidOperationResult = npgValidOperationResult.includes(doc.validationOperationResult);

   // Not handled case, ttl == -1, null or undefined
   if (!isLongTermStatus && !isShortTermStatus) {
        if(!ttl || ttl === "undefined" || ttl === null || ttl === -1){
            return true;
        }
   }

   // Check if the ttl exist
   if(!ttl){
       print(`ERROR: Doc ${doc._id}. TTL expected with a defined value but: ~${ttl}`);
       return false;
   }

   // Calculate the expired date and expected expired date

   // Calculate the date of expire starting from the current date plus the ttl, because the ttl is the seconds
   // from the current date when the document will be deleted
   const executionDate = new Date();
   let expiredDate = new Date(executionDate);
   expiredDate.setUTCSeconds(executionDate.getUTCSeconds() + ttl);

   let expectedExpiredDate = new Date(updateDate);
   let limitDate = new Date();

   if(isLongTermStatus || isNpgValidOperationResult){
        // Add 10 years
        expectedExpiredDate.setUTCFullYear(expectedExpiredDate.getUTCFullYear() + 10);
        limitDate.setFullYear(limitDate.getFullYear() - 10);
   }else if(isShortTermStatus){
        // Add 90 days
        expectedExpiredDate.setUTCDate(expectedExpiredDate.getUTCDate() + 90);
        limitDate.setUTCDate(limitDate.getUTCDate() - 90);
   }

   // Check if the document expectedExpiredDate is too old -> instantaneous delete 5 seconds
   if(limitDate.getTime() > expectedExpiredDate.getTime()){
        if(ttl === 5){
            return true;
        }else{
            print(`ERROR: Doc ${doc._id} with updateDate < limiteDate: ${limitDate.toUTCString()}. TTL expected 5, ttl: ~${ttl} s`);
            return false;
        }
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
