package com.defat.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defat.app.R
import com.defat.app.ui.theme.DefatTheme
import com.defat.core.domain.calc.MealGrouping
import com.defat.core.domain.model.Meal
import com.defat.core.domain.model.MealSource
import com.defat.core.domain.model.MealType
import com.defat.core.domain.model.MealTypeGroup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.defat.core.domain.calc.roundKcal

/**
 * The five meal-type sections shown on Home and History, in the order
 * [groups] arrives in — always [MealType] declaration order
 * (MealGrouping.groupByType). Shared by both screens so they cannot drift
 * (plan §8.4).
 */
@Composable
fun MealTypeSections(
    groups: List<MealTypeGroup>,
    onEditMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        groups.forEachIndexed { index, group ->
            MealTypeSection(group = group, onEditMeal = onEditMeal)
            if (index != groups.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MealTypeSection(group: MealTypeGroup, onEditMeal: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = mealTypeLabel(group.type), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.weight(1f))
            if (group.isEmpty) {
                Text(
                    text = stringResource(R.string.value_none),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.kcal_value, group.kcal.roundKcal()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (group.isEmpty) {
            Text(
                text = stringResource(R.string.home_group_empty),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column {
                group.meals.forEach { meal ->
                    MealTypeRow(meal = meal, onClick = { onEditMeal(meal.id) })
                }
            }
        }
    }
}

@Composable
private fun MealTypeRow(meal: Meal, onClick: () -> Unit) {
    val timeText = remember(meal.loggedAt) {
        meal.loggedAt.atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = meal.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.kcal_value, meal.kcal.roundKcal()),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun mealTypeLabel(type: MealType): String = when (type) {
    MealType.BREAKFAST -> stringResource(R.string.meal_type_breakfast)
    MealType.LUNCH -> stringResource(R.string.meal_type_lunch)
    MealType.AFTERNOON_TEA -> stringResource(R.string.meal_type_afternoon_tea)
    MealType.DINNER -> stringResource(R.string.meal_type_dinner)
    MealType.SNACK -> stringResource(R.string.meal_type_snack)
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MealTypeSectionsPreview() {
    val date = LocalDate.now()
    val meals = listOf(
        Meal(
            id = "1",
            loggedAt = Instant.now(),
            date = date,
            name = "Chicken and rice",
            kcal = 600.0,
            proteinG = 30.0,
            carbsG = 80.0,
            fatG = 15.0,
            source = MealSource.MANUAL,
            mealType = MealType.LUNCH,
        ),
    )
    DefatTheme {
        MealTypeSections(groups = MealGrouping.groupByType(meals), onEditMeal = {})
    }
}
