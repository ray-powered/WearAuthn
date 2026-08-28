package me.henneke.wearauthn.companion.ui.main

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import me.henneke.wearauthn.companion.R
import me.henneke.wearauthn.companion.databinding.MainFragmentBinding

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel by viewModels<MainViewModel>()

    private var _binding: MainFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MainFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val fab =
                activity?.findViewById<ExtendedFloatingActionButton>(R.id.floatingActionButton)
            if (scrollY > oldScrollY)
                fab?.hide()
            else
                fab?.show()
        })
        viewModel.isWatchAppInstalled.observe(viewLifecycleOwner) { isInstalled ->
            binding.installWatchAppCard.visibility = if (isInstalled) View.GONE else View.VISIBLE
        }
        @Suppress("DEPRECATION")
        binding.changelogView.text = if (Build.VERSION.SDK_INT >= 24)
            Html.fromHtml(getString(R.string.changelog), Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(getString(R.string.changelog))
    }

    override fun onResume() {
        super.onResume()
        viewModel.update()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

