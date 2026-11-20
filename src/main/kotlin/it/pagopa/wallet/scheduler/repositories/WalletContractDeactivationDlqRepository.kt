package it.pagopa.wallet.scheduler.repositories

import it.pagopa.wallet.documents.wallets.ContractDeactivationDlqDocument
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletContractDeactivationDlqRepository :
    ReactiveCrudRepository<ContractDeactivationDlqDocument, String>
