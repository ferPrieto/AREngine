package prieto.fernando.template.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_dashboard.*
import prieto.fernando.presentation.DashboardViewModel
import prieto.fernando.template.R
import prieto.fernando.template.ui.fragment.DashboardFragmentDirections.Companion.actionDashboardFragmentToBodyFragment
import prieto.fernando.template.ui.fragment.DashboardFragmentDirections.Companion.actionDashboardFragmentToFaceFragment
import prieto.fernando.template.ui.fragment.DashboardFragmentDirections.Companion.actionDashboardFragmentToHandFragment
import prieto.fernando.template.ui.fragment.DashboardFragmentDirections.Companion.actionDashboardFragmentToWorldFragment
import javax.inject.Inject


class DashboardFragment @Inject constructor(
    viewModelFactory: ViewModelProvider.Factory
) : Fragment(R.layout.fragment_dashboard) {
    private val viewModel by viewModels<DashboardViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewModelObservers()
        setClickListeners()
    }

    private fun setClickListeners() {
        button_body.setOnClickListener { viewModel.bodyClicked() }
        button_face.setOnClickListener { viewModel.faceClicked() }
        button_hand.setOnClickListener { viewModel.handClicked() }
        button_world.setOnClickListener { viewModel.worldClicked() }
    }

    private fun setViewModelObservers() {
        viewModel.goToBody.observe(viewLifecycleOwner, Observer { navigateToBody() })
        viewModel.goToFace.observe(viewLifecycleOwner, Observer { navigateToFace() })
        viewModel.goToHand.observe(viewLifecycleOwner, Observer { navigateToHand() })
        viewModel.goToWorld.observe(viewLifecycleOwner, Observer { navigateToWorld() })
    }

    private fun navigateToBody() {
        findNavController().navigate(actionDashboardFragmentToBodyFragment())
    }

    private fun navigateToFace() {
        findNavController().navigate(actionDashboardFragmentToFaceFragment())
    }

    private fun navigateToHand() {
        findNavController().navigate(actionDashboardFragmentToHandFragment())
    }

    private fun navigateToWorld() {
        findNavController().navigate(actionDashboardFragmentToWorldFragment())
    }
}