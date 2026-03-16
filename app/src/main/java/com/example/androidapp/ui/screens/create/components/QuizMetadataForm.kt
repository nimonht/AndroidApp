package com.example.androidapp.ui.screens.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.ui.components.forms.TextInputField
import com.example.androidapp.ui.components.forms.SwitchToggle
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun QuizMetadataForm(
    title: String,
    description: String,
    tags: String,
    isPublic: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextInputField(
            value = title,
            onValueChange = onTitleChange,
            label = stringResource(R.string.quiz_title)
        )

        TextInputField(
            value = description,
            onValueChange = onDescriptionChange,
            label = stringResource(R.string.quiz_description),
            singleLine = false
        )

        TextInputField(
            value = tags,
            onValueChange = onTagsChange,
            label = stringResource(R.string.quiz_tags)
        )

        SwitchToggle(
            checked = isPublic,
            onCheckedChange = onPublicChange,
            label = stringResource(R.string.quiz_public)
        )
    }
}
