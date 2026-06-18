package com.example.frenchquiz.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.frenchquiz.domain.QuizMode
import com.example.frenchquiz.ui.HomeUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStart: (category: String?, mode: QuizMode) -> Unit,
    onClearProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMode by remember { mutableStateOf(QuizMode.RANDOM) }
    // null = すべてのカテゴリ
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("仏検2級 問題演習", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "とにかく解きまくって、苦手だけ復習しよう。",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        StatsCard(state)

        Spacer(Modifier.height(20.dp))
        Text("出題モード", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedMode == QuizMode.RANDOM,
                onClick = { selectedMode = QuizMode.RANDOM },
                label = { Text("ランダム") },
            )
            FilterChip(
                selected = selectedMode == QuizMode.ALL,
                onClick = { selectedMode = QuizMode.ALL },
                label = { Text("すべて順番に") },
            )
            FilterChip(
                selected = selectedMode == QuizMode.INCORRECT_ONLY,
                enabled = state.incorrectCount > 0,
                onClick = { selectedMode = QuizMode.INCORRECT_ONLY },
                label = { Text("不正解のみ (${state.incorrectCount})") },
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("カテゴリ", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("すべて") },
            )
            state.categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onStart(selectedCategory, selectedMode) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("開始する")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onClearProgress,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("学習記録をリセット")
        }
    }
}

@Composable
private fun StatsCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem("総問題数", state.totalCount)
            StatItem("正解済み", state.correctCount)
            StatItem("要復習", state.incorrectCount)
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(2.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
