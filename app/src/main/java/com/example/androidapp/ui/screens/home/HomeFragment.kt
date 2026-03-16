package com.example.androidapp.ui.screens.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.androidapp.QuizzezApplication
import com.example.androidapp.R
import com.example.androidapp.databinding.FragmentHomeBinding
import com.example.androidapp.databinding.ItemMyQuizBinding
import com.example.androidapp.databinding.ItemRecentlyPlayedBinding
import com.example.androidapp.domain.model.Quiz
import kotlinx.coroutines.launch

/**
 * Home dashboard Fragment - XML implementation.
 * Displays recently played quizzes, user's quizzes, and trending quizzes.
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

    private lateinit var recentlyPlayedAdapter: RecentlyPlayedAdapter
    private lateinit var myQuizzesAdapter: MyQuizzesAdapter
    private lateinit var trendingAdapter: RecentlyPlayedAdapter

    private var onNavigateToQuiz: ((String) -> Unit)? = null
    private var onNavigateToSearch: (() -> Unit)? = null

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

        setupRecyclerViews()
        setupJoinCodeInput()
        setupClickListeners()
        observeUiState()
    }

    private fun setupRecyclerViews() {
        // Recently Played RecyclerView
        recentlyPlayedAdapter = RecentlyPlayedAdapter { quizId ->
            onNavigateToQuiz?.invoke(quizId)
        }
        binding.recentlyPlayedRecyclerView.apply {
            adapter = recentlyPlayedAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // My Quizzes RecyclerView
        myQuizzesAdapter = MyQuizzesAdapter { quizId ->
            onNavigateToQuiz?.invoke(quizId)
        }
        binding.myQuizzesRecyclerView.apply {
            adapter = myQuizzesAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Trending RecyclerView
        trendingAdapter = RecentlyPlayedAdapter { quizId ->
            onNavigateToQuiz?.invoke(quizId)
        }
        binding.trendingRecyclerView.apply {
            adapter = trendingAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupJoinCodeInput() {
        binding.joinCodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val code = s?.toString() ?: ""
                viewModel.onEvent(HomeEvent.JoinCodeChanged(code))
                binding.joinButton.isEnabled = code.length == 6
            }
        })
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onEvent(HomeEvent.Refresh)
        }

        binding.joinButton.setOnClickListener {
            val code = binding.joinCodeInput.text.toString()
            viewModel.onEvent(HomeEvent.JoinQuiz(code))
        }

        binding.recentlyPlayedSeeAll.setOnClickListener {
            onNavigateToSearch?.invoke()
        }

        binding.myQuizzesSeeAll.setOnClickListener {
            onNavigateToSearch?.invoke()
        }

        binding.trendingSeeAll.setOnClickListener {
            onNavigateToSearch?.invoke()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUi(state)
            }
        }
    }

    private fun updateUi(state: HomeUiState) {
        // Refresh indicator
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing

        // Welcome section
        if (state.displayName.isNotEmpty()) {
            binding.welcomeTitle.text = getString(R.string.home_welcome_display, state.displayName)
        } else {
            binding.welcomeTitle.text = getString(R.string.home_greeting)
        }

        // Join code section
        binding.joinButton.isEnabled = state.joinCode.length == 6 && !state.isJoining
        if (state.joinCodeError != null) {
            binding.joinCodeError.visibility = View.VISIBLE
            binding.joinCodeError.text = state.joinCodeError
        } else {
            binding.joinCodeError.visibility = View.GONE
        }

        // Recently Played
        if (state.recentQuizzes.isEmpty()) {
            binding.recentlyPlayedRecyclerView.visibility = View.GONE
            binding.recentlyPlayedEmpty.visibility = View.VISIBLE
        } else {
            binding.recentlyPlayedRecyclerView.visibility = View.VISIBLE
            binding.recentlyPlayedEmpty.visibility = View.GONE
            recentlyPlayedAdapter.submitList(state.recentQuizzes)
        }

        // My Quizzes
        if (state.myQuizzes.isEmpty()) {
            binding.myQuizzesRecyclerView.visibility = View.GONE
            binding.myQuizzesEmpty.visibility = View.VISIBLE
        } else {
            binding.myQuizzesRecyclerView.visibility = View.VISIBLE
            binding.myQuizzesEmpty.visibility = View.GONE
            myQuizzesAdapter.submitList(state.myQuizzes)
        }

        // Trending
        if (state.trendingQuizzes.isEmpty()) {
            binding.trendingRecyclerView.visibility = View.GONE
            binding.trendingEmpty.visibility = View.VISIBLE
        } else {
            binding.trendingRecyclerView.visibility = View.VISIBLE
            binding.trendingEmpty.visibility = View.GONE
            trendingAdapter.submitList(state.trendingQuizzes)
        }

        // Navigate to quiz if join was successful
        if (state.joinedQuizId != null) {
            onNavigateToQuiz?.invoke(state.joinedQuizId)
            viewModel.onEvent(HomeEvent.ClearJoinResult)
            binding.joinCodeInput.text?.clear()
        }
    }

    fun setNavigationCallbacks(
        onNavigateToQuiz: (String) -> Unit,
        onNavigateToSearch: () -> Unit
    ) {
        this.onNavigateToQuiz = onNavigateToQuiz
        this.onNavigateToSearch = onNavigateToSearch
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter for Recently Played and Trending quizzes (horizontal carousel).
 */
class RecentlyPlayedAdapter(
    private val onQuizClick: (String) -> Unit
) : RecyclerView.Adapter<RecentlyPlayedAdapter.ViewHolder>() {

    private var quizzes: List<Quiz> = emptyList()

    fun submitList(newQuizzes: List<Quiz>) {
        quizzes = newQuizzes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentlyPlayedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(quizzes[position])
    }

    override fun getItemCount(): Int = quizzes.size

    inner class ViewHolder(private val binding: ItemRecentlyPlayedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onQuizClick(quizzes[position].id)
                }
            }
        }

        fun bind(quiz: Quiz) {
            binding.quizTitle.text = quiz.title
            binding.quizQuestionCount.text =
                binding.root.context.getString(R.string.quiz_questions, quiz.questionCount)

            if (quiz.thumbnailUrl != null) {
                binding.quizThumbnail.load(quiz.thumbnailUrl)
            } else {
                binding.quizThumbnail.setImageDrawable(null)
            }
        }
    }
}

/**
 * Adapter for My Quizzes list (vertical list).
 */
class MyQuizzesAdapter(
    private val onQuizClick: (String) -> Unit
) : RecyclerView.Adapter<MyQuizzesAdapter.ViewHolder>() {

    private var quizzes: List<Quiz> = emptyList()

    fun submitList(newQuizzes: List<Quiz>) {
        quizzes = newQuizzes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyQuizBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(quizzes[position], position < quizzes.size - 1)
    }

    override fun getItemCount(): Int = quizzes.size

    inner class ViewHolder(private val binding: ItemMyQuizBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onQuizClick(quizzes[position].id)
                }
            }
        }

        fun bind(quiz: Quiz, showDivider: Boolean) {
            binding.quizTitle.text = quiz.title
            binding.quizQuestionCount.text =
                binding.root.context.getString(R.string.quiz_questions, quiz.questionCount)
            binding.divider.visibility = if (showDivider) View.VISIBLE else View.GONE
        }
    }
}
