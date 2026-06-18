package com.example.frenchquiz.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.frenchquiz.data.model.QuizItem
import com.example.frenchquiz.ui.QuizUiState
import com.example.frenchquiz.ui.theme.CorrectGreen
import com.example.frenchquiz.ui.theme.IncorrectRed

@Composable
fun QuizScreen(
    state: QuizUiState,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current
    if (item == null) {
        EmptySession(onQuit, modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        // ヘッダー：進捗
        Text(
            "${item.category}  ・  ${state.index + 1} / ${state.total}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (state.index + 1).toFloat() / state.total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        item.instruction?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
        }
        item.contextText?.let {
            if (item.type == com.example.frenchquiz.data.model.QuestionType.READING) {
                Text("本文", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
        }

        Text(item.prompt, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

        item.hintJa?.let {
            Spacer(Modifier.height(8.dp))
            Text("（$it）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }

        Spacer(Modifier.height(20.dp))

        // 選択肢
        item.choices.forEachIndexed { i, choice ->
            ChoiceRow(
                text = choice,
                index = i,
                selected = state.selected == i,
                answered = state.answered,
                isCorrect = i == item.answerIndex,
                onClick = { onSelect(i) },
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        if (!state.answered) {
            Button(
                onClick = onSubmit,
                enabled = state.selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("解答する")
            }
        } else {
            ExplanationCard(item = item, selected = state.selected)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isLast) "結果を見る" else "次の問題へ")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onQuit, modifier = Modifier.fillMaxWidth()) {
            Text("中断してホームへ")
        }
    }
}

@Composable
private fun ChoiceRow(
    text: String,
    index: Int,
    selected: Boolean,
    answered: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit,
) {
    // 解答後の配色
    val borderColor: Color
    val bg: Color
    when {
        answered && isCorrect -> {
            borderColor = CorrectGreen; bg = CorrectGreen.copy(alpha = 0.12f)
        }
        answered && selected && !isCorrect -> {
            borderColor = IncorrectRed; bg = IncorrectRed.copy(alpha = 0.12f)
        }
        selected -> {
            borderColor = MaterialTheme.colorScheme.primary; bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        }
        else -> {
            borderColor = MaterialTheme.colorScheme.outline; bg = Color.Transparent
        }
    }

    val label = ('A' + index)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !answered, onClick = onClick),
    ) {
        Box(Modifier.padding(14.dp)) {
            Text("$label.  $text", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ExplanationCard(item: QuizItem, selected: Int?) {
    val correct = selected == item.answerIndex
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (correct) "正解！" else "不正解",
                style = MaterialTheme.typography.titleLarge,
                color = if (correct) CorrectGreen else IncorrectRed,
            )
            Spacer(Modifier.height(6.dp))
            Text("正解: ${('A' + item.answerIndex)}.  ${item.answerText}", fontWeight = FontWeight.Medium)
            item.grammarPoint?.let {
                Spacer(Modifier.height(6.dp))
                Text("ポイント: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(10.dp))
            Text(item.explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptySession(onQuit: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("出題できる問題がありません。", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "選んだ条件に該当する問題がありませんでした。条件を変えてお試しください。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onQuit) { Text("ホームへ戻る") }
    }
}
