package project.moms.attractions.presentation.partWithMap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import project.moms.attractions.databinding.FullScreenItemBinding
import project.moms.attractions.model.Element

class FragmentFullScreenItem : Fragment() {
    private var _binding : FullScreenItemBinding? = null
    private val binding : FullScreenItemBinding
        get() {return _binding!!}

    private var param: Element? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FullScreenItemBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val marker = arguments?.getParcelable<Element>(KEY_MARKER)
        marker?.let {
            // Используйте информацию о маркере для отображения на экране
            val name = it.tags["name:en"] ?: "Unknown"
            binding.fieldHeader.text = name
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_MARKER = "KEY_MARKER"
    }
}