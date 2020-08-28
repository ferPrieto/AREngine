package prieto.fernando.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

abstract class DashboardViewModel : ViewModel() {
    abstract fun bodyClicked()
    abstract fun faceClicked()
    abstract fun handClicked()
    abstract fun worldClicked()

    abstract val goToBody: LiveData<Unit>
    abstract val goToFace: LiveData<Unit>
    abstract val goToHand: LiveData<Unit>
    abstract val goToWorld: LiveData<Unit>
}

class DashboardViewModelImpl @Inject constructor() : DashboardViewModel() {
    private val _goToBody = MediatorLiveData<Unit>()
    private val _goToFace = MediatorLiveData<Unit>()
    private val _goToHand = MediatorLiveData<Unit>()
    private val _goToWorld = MediatorLiveData<Unit>()

    override val goToBody: LiveData<Unit>
        get() = _goToBody
    override val goToFace: LiveData<Unit>
        get() = _goToFace
    override val goToHand: LiveData<Unit>
        get() = _goToHand
    override val goToWorld: LiveData<Unit>
        get() = _goToWorld

    override fun bodyClicked() {
        _goToBody.postValue(Unit)
    }

    override fun faceClicked() {
        _goToFace.postValue(Unit)
    }

    override fun handClicked() {
        _goToHand.postValue(Unit)
    }

    override fun worldClicked() {
        _goToWorld.postValue(Unit)
    }
}
