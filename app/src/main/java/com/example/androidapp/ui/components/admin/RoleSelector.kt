package com.example.androidapp.ui.components.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidapp.R
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Dropdown selector for user role selection in admin panels.
 *
 * @param selectedRole The currently selected role.
 * @param onRoleSelected Callback when a role is selected.
 * @param modifier Modifier for styling.
 * @param enabled Whether the selector is enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = when (selectedRole) {
                UserRole.ADMIN -> stringResource(R.string.admin_role_admin)
                UserRole.USER -> stringResource(R.string.admin_role_user)
                UserRole.GUEST -> stringResource(R.string.admin_role_guest)
            },
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = {
                Text(
                    text = stringResource(R.string.admin_role_label),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.admin_role_admin),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium
                    )
                },
                onClick = {
                    onRoleSelected(UserRole.ADMIN)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.admin_role_user),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium
                    )
                },
                onClick = {
                    onRoleSelected(UserRole.USER)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.admin_role_guest),
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium
                    )
                },
                onClick = {
                    onRoleSelected(UserRole.GUEST)
                    expanded = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RoleSelectorPreview() {
    QuizzezTheme {
        RoleSelector(
            selectedRole = UserRole.USER,
            onRoleSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
