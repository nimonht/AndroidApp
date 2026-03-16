package com.example.androidapp.ui.screens.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentHomeBinding
import com.example.androidapp.di.AppContainerImpl
import com.example.androidapp.di.QuizzezApplication
import kotlinx.coroutines.launch

/**
 * HomeFragment - XML-based implementation of the Home screen.
 * Demonstrates hybrid Compose/XML architecture.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContainer = (requireActivity().application as QuizzezApplication).appContainer
                return HomeViewModel(appContainer.quizRepository, appContainer.authRepository) as T
            }
        }
    }

    private lateinit var recentlyPlayedAdapter: QuizCardHorizontalAdapter
    private lateinit var myQuizzesAdapter: QuizListAdapter
    private lateinit var trendingAdapter: QuizCardHorizontalAdapter

    var onNavigateToQuiz: ((String) -> Unit)? = null
    var onNavigateToSearch: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onEvent(HomeEvent.Refresh)
        }

        // Setup Join Code input
        binding.etJoinCode.doAfterTextChanged { text ->
            viewModel.onEvent(HomeEvent.JoinCodeChanged(text.toString()))
        }

        binding.btnJoin.setOnClickListener {
            viewModel.onEvent(HomeEvent.JoinQuiz(binding.etJoinCode.text.toString()))
        }

        // Setup RecyclerViews
        recentlyPlayedAdapter = QuizCardHorizontalAdapter { quizId ->
            onNavigateToQuiz?.invoke(quizId)
        }
        binding.rvRecentlyPlayed.apply {
            adapter = recentlyPlayedAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        myQuizzesAdapter = QuizListAdapter { quizId ->
            onNavigateToQuiz?.invoke(quizId)
        }
        binding.rvMyQuizzes.apply {
            adapter = myQuizzesAdapter
            layoutManager = LinearLayoutManager(context)
        }

        trendingAdapter = QuizCardHorizontalAdapter { quizId ->
            onNavigateToQuiz?.invoke(quizId)
        }
        binding.rvTrending.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update UI based on state
                binding.swipeRefresh.isRefreshing = state.isRefreshing

                // Update welcome message
                if (state.displayName.isNotEmpty()) {
                    binding.tvWelcomeTitle.text = getString(R.string.home_welcome_display, state.displayName)
                } else {
                    binding.tvWelcomeTitle.text = getString(R.string.home_greeting)
                }

                // Update join code error
                binding.tvJoinError.isVisible = state.joinCodeError != null
                binding.tvJoinError.text = state.joinCodeError

                // Update Recently Played
                recentlyPlayedAdapter.submitList(state.recentQuizzes)
                binding.rvRecentlyPlayed.isVisible = state.recentQuizzes.isNotEmpty()
                binding.tvRecentlyPlayedEmpty.isVisible = state.recentQuizzes.isEmpty()

                // Update My Quizzes
                myQuizzesAdapter.submitList(state.myQuizzes)
                binding.rvMyQuizzes.isVisible = state.myQuizzes.isNotEmpty()
                binding.tvMyQuizzesEmpty.isVisible = state.myQuizzes.isEmpty()

                // Update Trending
                trendingAdapter.submitList(state.trendingQuizzes)
                binding.rvTrending.isVisible = state.trendingQuizzes.isNotEmpty()
                binding.tvTrendingEmpty.isVisible = state.trendingQuizzes.isEmpty()

                // Handle navigation to quiz
                state.joinedQuizId?.let { quizId ->
                    onNavigateToQuiz?.invoke(quizId)
                    viewModel.onEvent(HomeEvent.ClearJoinResult)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
