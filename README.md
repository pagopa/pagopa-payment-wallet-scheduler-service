# pagopa-payment-wallet-scheduler-service
This application is designed to define and execute every scheduled process related to payment wallet's domain

## Api Documentation 📖

See
[//]CHANGE IT: the [OpenAPI 3 here.](https://editor.swagger.io/?url=https://raw.githubusercontent.com/pagopa/pagopa-wallet-service/main/api-spec/wallet-api.yaml)

---

## Technology Stack

- Kotlin
- Spring Boot

---

## Start Project Locally 🚀

### Prerequisites

- docker

### Populate the environment

The microservice needs a valid `.env` file in order to be run.

If you want to start the application without too much hassle, you can just copy `.env.local` with

```shell
$ cp .env.local .env
```

to get a good default configuration.

If you want to customize the application environment, reference this table:


| Variable name                                  | Description                                                                                                                                                | type              | default |
|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|---------|
| MONGO_HOST                                     | Host where MongoDB instance used to persist wallet data                                                                                                    | hostname (string) |         |
| MONGO_PORT                                     | Port where MongoDB is bound to in MongoDB host                                                                                                             | number            |         |
| MONGO_USERNAME                                 | MongoDB username used to connect to the database                                                                                                           | string            |         |
| MONGO_PASSWORD                                 | MongoDB password used to connect to the database                                                                                                           | string            |         |
| MONGO_SSL_ENABLED                              | Whether SSL is enabled while connecting to MongoDB                                                                                                         | string            |         |
| MONGO_DB_NAME                                  | Mongo database name                                                                                                                                        | string            |         |
| MONGO_MIN_POOL_SIZE                            | Min amount of connections to be retained into connection pool. See docs *                                                                                  | string            |         |
| MONGO_MAX_POOL_SIZE                            | Max amount of connections to be retained into connection pool.See docs *                                                                                   | string            |         |
| MONGO_MAX_IDLE_TIMEOUT_MS                      | Max timeout after which an idle connection is killed in milliseconds. See docs *                                                                           | string            |         |
| MONGO_CONNECTION_TIMEOUT_MS                    | Max time to wait for a connection to be opened. See docs *                                                                                                 | string            |         |
| MONGO_SOCKET_TIMEOUT_MS                        | Max time to wait for a command send or receive before timing out. See docs *                                                                               | string            |         |
| MONGO_SERVER_SELECTION_TIMEOUT_MS              | Max time to wait for a server to be selected while performing a communication with Mongo in milliseconds. See docs *                                       | string            |         |
| MONGO_WAITING_QUEUE_MS                         | Max time a thread has to wait for a connection to be available in milliseconds. See docs *                                                                 | string            |         |
| MONGO_HEARTBEAT_FREQUENCY_MS                   | Hearth beat frequency in milliseconds. This is an hello command that is sent periodically on each active connection to perform an health check. See docs * | string            |         |
| MONGO_REPLICA_SET_OPTION                       | The replica set connection string option valued with the name of the replica set. See docs *                                                               | string            |         |
| ROOT_LOGGING_LEVEL                             | Application root logger level                                                                                                                              | string            | INFO    |
| APP_LOGGING_LEVEL                              | it.pagopa logger level                                                                                                                                     | string            | INFO    |
| WEB_LOGGING_LEVEL                              | Web logger level                                                                                                                                           | string            | DEBUG   |
| SCHEDULER_CDC_QUEUE_NAME                       | Name of change data capture queue                                                                                                                          | string            |         |
| SCHEDULER_CDC_QUEUE_TTL_SECONDS                | TTL in seconds for published message                                                                                                                       | string            |         |
| SCHEDULER_CDC_QUEUE_CONNECTION_STRING          | Connection string to storage queue                                                                                                                         | string            |         |
| SCHEDULER_CDC_QUEUE_VISIBILITY_TIMEOUT_SECONDS | Visibility timeout in seconds for expired event                                                                                                            |                   |         |
| SCHEDULER_CDC_SEND_RETRY_MAX_ATTEMPTS          | Max configurable attempts for performing the logic business related to a change event                                                                      | long              |         |
| SCHEDULER_CDC_SEND_RETRY_INTERVAL_IN_MS        | Configurable interval in milliseconds between retries attempts                                                                                             | long              |         |
| WALLET_SEARCH_STATUS                           | Wallet status search query for cdc injection                                                                                                                | string            |         |
| WALLET_SEARCH_LIMIT                            | Wallet limit search query for cdc injection                                                                                                                 | int               |         |
| SCHEDULER_REDIS_RESUME_KEYSPACE                | Prefix used for redis key name                                                                                                                              | string            |         |
| SCHEDULER_REDIS_RESUME_FALLBACK_IN_MIN         | Fallbacks in minutes before now in case there is no resume token in cache                                                                                   | long              |         |
| REDIS_HOST                                     | Host of redis                                                                                                                                               | hostname (string) | test    | 
| REDIS_PORT                                     | Port of redis                                                                                                                                               | number            | 6380    |
| REDIS_SSL_ENABLED                              | Redis should use SSL                                                                                                                                        | boolean           | true    |
| PAYMENT_WALLET_JOB_EXECUTION_CRON              | Payment wallet processing batch execution cron expression                                                                                                  | string            |         |
| PAYMENT_WALLET_JOB_EXECUTION_START_DATE        | Payment wallet processing batch start date (considering wallet creation date) in ISO-8601 format (ex. 1970-01-01T00:00:00Z)                                | datetime (string) |         |
| PAYMENT_WALLET_JOB_EXECUTION_END_DATE          | Payment wallet processing batch end date (considering wallet creation date) in ISO-8601 format (ex. 1970-01-01T00:00:00Z)                                  | datetime (string) |         |


(*): for Mongo connection string options
see [docs](https://www.mongodb.com/docs/drivers/java/sync/v4.3/fundamentals/connection/connection-options/#connection-options)

### Run docker container

```shell
$ docker compose up --build
```

---

## Develop Locally 💻

### Prerequisites

- git
- gradle
- jdk-21

### Run the project

```shell
$ export $(grep -v '^#' .env.local | xargs)
$ ./gradlew bootRun
```

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

```shell
$ ./gradlew test
```

#### Integration testing

TODO

#### Performance testing

TODO

### Dependency management 🔧

For support reproducible build this project has the following gradle feature enabled:

- [dependency lock](https://docs.gradle.org/8.1/userguide/dependency_locking.html)
- [dependency verification](https://docs.gradle.org/8.1/userguide/dependency_verification.html)

#### Dependency lock

This feature use the content of `gradle.lockfile` to check the declared dependencies against the locked one.

If a transitive dependencies have been upgraded the build will fail because of the locked version mismatch.

The following command can be used to upgrade dependency lockfile:

```shell
./gradlew dependencies --write-locks 
```

Running the above command will cause the `gradle.lockfile` to be updated against the current project dependency
configuration

#### Dependency verification

This feature is enabled by adding the gradle `./gradle/verification-metadata.xml` configuration file.

Perform checksum comparison against dependency artifact (jar files, zip, ...) and metadata (pom.xml, gradle module
metadata, ...) used during build
and the ones stored into `verification-metadata.xml` file raising error during build in case of mismatch.

The following command can be used to recalculate dependency checksum:

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

In the above command the `clean`, `spotlessApply` `build` tasks where chosen to be run
in order to discover all transitive dependencies used during build and also the ones used during
spotless apply task used to format source code.

The above command will upgrade the `verification-metadata.xml` adding all the newly discovered dependencies' checksum.
Those checksum should be checked against a trusted source to check for corrispondence with the library author published
checksum.

`/gradlew --write-verification-metadata sha256` command appends all new dependencies to the verification files but does
not remove
entries for unused dependencies.

This can make this file grow every time a dependency is upgraded.

To detect and remove old dependencies make the following steps:

1. Delete, if present, the `gradle/verification-metadata.dryrun.xml`
2. Run the gradle write-verification-metadata in dry-mode (this will generate a verification-metadata-dryrun.xml file
   leaving untouched the original verification file)
3. Compare the verification-metadata file and the verification-metadata.dryrun one checking for differences and removing
   old unused dependencies

The 1-2 steps can be performed with the following commands

```Shell
rm -f ./gradle/verification-metadata.dryrun.xml 
./gradlew --write-verification-metadata sha256 clean spotlessApply build --dry-run
```

The resulting `verification-metadata.xml` modifications must be reviewed carefully checking the generated
dependencies checksum against official websites or other secure sources.

If a dependency is not discovered during the above command execution it will lead to build errors.

You can add those dependencies manually by modifying the `verification-metadata.xml`
file adding the following component:

```xml

<verification-metadata>
    <!-- other configurations... -->
    <components>
        <!-- other components -->
        <component group="GROUP_ID" name="ARTIFACT_ID" version="VERSION">
            <artifact name="artifact-full-name.jar">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
            <artifact name="artifact-pom-file.pom">
                <sha256 value="sha value"
                        origin="Description of the source of the checksum value"/>
            </artifact>
        </component>
    </components>
</verification-metadata>
```

Add those components at the end of the components list and then run the

```shell
./gradlew --write-verification-metadata sha256 clean spotlessApply build --no-build-cache --refresh-dependencies
```

that will reorder the file with the added dependencies checksum in the expected order.

Finally, you can add new dependencies both to gradle.lockfile writing verification metadata running

```shell
 ./gradlew dependencies --write-locks --write-verification-metadata sha256 --no-build-cache --refresh-dependencies
```

For more information read the
following [article](https://docs.gradle.org/8.1/userguide/dependency_verification.html#sec:checksum-verification)

## Contributors 👥

Made with ❤️ by PagoPA S.p.A.

### Maintainers

See `CODEOWNERS` file