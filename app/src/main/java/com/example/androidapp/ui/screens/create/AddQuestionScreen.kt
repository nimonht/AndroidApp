package com.example.androidapp.ui.screens.create



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.androidapp.R
import com.example.androidapp.ui.components.forms.TextInputField

import com.example.androidapp.ui.screens.create.CreateQuizViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddQuestionScreen(
    onNavigateBack: () -> Unit,

    modifier: Modifier = Modifier
) {
    val viewModel: CreateQuizViewModel = viewModel()
    val question by viewModel.questionDraft.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextInputField(
            value = question.content,
            onValueChange = {
                viewModel.onEvent(
                    CreateQuizEvent.UpdateQuestionContent(it)
                )
            },
            label = stringResource(R.string.question_content)
        )

        TextInputField(
            value = question.mediaUrl ?: "",
            onValueChange = {
                viewModel.onEvent(
                    CreateQuizEvent.UpdateQuestionMedia(it)
                )
            },
            label = stringResource(R.string.question_media)
        )

        TextInputField(
            value = question.points.toString(),
            onValueChange = {

                val points = it.toIntOrNull() ?: 1

                viewModel.onEvent(
                    CreateQuizEvent.UpdateQuestionPoints(points)
                )
            },
            label = stringResource(R.string.question_points)
        )

        TextInputField(
            value = question.explanation,
            onValueChange = {
                viewModel.onEvent(
                    CreateQuizEvent.UpdateExplanation(it)
                )
            },
            label = stringResource(R.string.question_explanation)
        )

        Button(
            onClick = {
                viewModel.onEvent(CreateQuizEvent.SaveQuestion)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.add_question))
        }

    }
}
