package prieto.fernando.template.ui.activity

import android.os.Bundle
import androidx.fragment.app.FragmentFactory
import androidx.navigation.Navigation
import dagger.android.support.DaggerAppCompatActivity
import prieto.fernando.template.R
import javax.inject.Inject

class ConnectAppMarketActivity : DaggerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }
}
