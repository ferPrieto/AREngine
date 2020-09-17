package prieto.fernando.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    override val goToBody: MutableLiveData<Unit> = MutableLiveData()
    override val goToFace: MutableLiveData<Unit> = MutableLiveData()
    override val goToHand: MutableLiveData<Unit> = MutableLiveData()
    override val goToWorld: MutableLiveData<Unit> = MutableLiveData()

    override fun bodyClicked() {
        goToBody.postValue(Unit)
    }

    override fun faceClicked() {
        goToFace.postValue(Unit)
    }

    override fun handClicked() {
        goToHand.postValue(Unit)
    }

    override fun worldClicked() {
        goToWorld.postValue(Unit)
    }
}
