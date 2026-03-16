package com.example.androidapp.ui.screens.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.androidapp.R
import com.example.androidapp.databinding.ItemQuizListBinding
import com.example.androidapp.domain.model.Quiz

/**
 * RecyclerView adapter for quiz list items (My Quizzes).
 */
class QuizListAdapter(
    private val onQuizClick: (String) -> Unit
) : ListAdapter<Quiz, QuizListAdapter.ViewHolder>(QuizDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuizListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemQuizListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(quiz: Quiz) {
            binding.tvQuizTitle.text = quiz.title
            binding.tvQuizInfo.text = binding.root.context.getString(
                R.string.quiz_questions,
                quiz.questions.size
            )
            binding.root.setOnClickListener {
                onQuizClick(quiz.id)
            }
        }
    }

    private class QuizDiffCallback : DiffUtil.ItemCallback<Quiz>() {
        override fun areItemsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Quiz, newItem: Quiz): Boolean {
            return oldItem == newItem
        }
    }
}
