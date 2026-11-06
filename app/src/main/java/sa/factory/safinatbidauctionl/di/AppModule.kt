package sa.factory.safinatbidauctionl.di

import sa.factory.safinatbidauctionl.data.repository.SafinatBidRepositoryImpl
import sa.factory.safinatbidauctionl.domain.repository.SafinatBidRepository
import sa.factory.safinatbidauctionl.domain.usecase.*
import sa.factory.safinatbidauctionl.hardware.HardwareSensorDriver
import sa.factory.safinatbidauctionl.ui.viewmodel.SafinatBidViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: SafinatBidRepository = SafinatBidRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): SafinatBidViewModel = SafinatBidViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
