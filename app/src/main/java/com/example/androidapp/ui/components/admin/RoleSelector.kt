package com.example.androidapp.ui.components.admin

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * Dropdown selector for user role selection in admin panels.
 *
 * Renders a Material 3 [ExposedDropdownMenuBox] with one item per [UserRole].
 * The SUPERUSER option is hidden by default and only shown when
 * [showSuperuser] is `true` (i.e. the current user is a superuser).
 * Any roles listed in [excludeRoles] are omitted from the dropdown.
 *
 * @param selectedRole The currently selected role.
 * @param onRoleSelected Callback when a role is selected.
 * @param modifier Modifier for styling.
 * @param enabled Whether the selector is enabled.
 * @param showSuperuser Whether to include the SUPERUSER option in the dropdown.
 * @param excludeRoles Roles that should be hidden from the dropdown options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSuperuser: Boolean = false,
    excludeRoles: Set<UserRole> = emptySet()
) {
    var expanded by remember { mutableStateOf(false) }

    val availableRoles = remember(showSuperuser, excludeRoles) {
        UserRole.entries.filter { role ->
            (role != UserRole.SUPERUSER || showSuperuser) && role !in excludeRoles
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded && enabled },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = roleDisplayText(selectedRole),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = {
                Text(
                    text = stringResource(R.string.admin_role_label),
                    fontWeight = FontWeight.Medium
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableRoles.forEach { role ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = roleDisplayText(role),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        onRoleSelected(role)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Returns the localised display text for a [UserRole].
 */
@Composable
private fun roleDisplayText(role: UserRole): String = when (role) {
    UserRole.SUPERUSER -> stringResource(R.string.admin_role_superuser)
    UserRole.ADMIN -> stringResource(R.string.admin_role_admin)
    UserRole.USER -> stringResource(R.string.admin_role_user)
    UserRole.GUEST -> stringResource(R.string.admin_role_guest)
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, name = "Light")
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

@Preview(showBackground = true, name = "Light - With Superuser")
@Composable
private fun RoleSelectorWithSuperuserPreview() {
    QuizzezTheme {
        RoleSelector(
            selectedRole = UserRole.ADMIN,
            onRoleSelected = {},
            showSuperuser = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(
    showBackground = true,
    name = "Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RoleSelectorDarkPreview() {
    QuizzezTheme {
        RoleSelector(
            selectedRole = UserRole.USER,
            onRoleSelected = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
