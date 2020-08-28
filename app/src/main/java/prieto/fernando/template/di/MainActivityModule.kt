package prieto.fernando.template.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import prieto.fernando.core.di.FragmentKey
import prieto.fernando.core.di.ViewModelKey
import prieto.fernando.presentation.DashboardViewModel
import prieto.fernando.presentation.DashboardViewModelImpl
import prieto.fernando.template.ui.*
import prieto.fernando.template.ui.fragment.*

@Module
internal abstract class MainActivityModule {
    @ActivityScope
    @ContributesAndroidInjector
    abstract fun bindMainActivity(): MainActivity

    @Binds
    @IntoMap
    @FragmentKey(DashboardFragment::class)
    abstract fun bindDashboardFragment(dashboardFragment: DashboardFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(BodyFragment::class)
    abstract fun bindBodyFragment(bodyFragment: BodyFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(FaceFragment::class)
    abstract fun bindFaceFragment(faceFragment: FaceFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(HandFragment::class)
    abstract fun bindHandFragment(handFragment: HandFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(WorldFragment::class)
    abstract fun bindWorldFragment(worldFragment: WorldFragment): Fragment

    @Binds
    @IntoMap
    @ViewModelKey(DashboardViewModel::class)
    abstract fun bindDashboardViewModelImpl(viewModel: DashboardViewModelImpl): ViewModel
}
