package com.example.frenchquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frenchquiz.domain.QuizMode
import com.example.frenchquiz.ui.QuizViewModel
import com.example.frenchquiz.ui.home.HomeScreen
import com.example.frenchquiz.ui.quiz.QuizScreen
import com.example.frenchquiz.ui.result.ResultScreen
import com.example.frenchquiz.ui.theme.FrenchQuizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrenchQuizTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val vm: QuizViewModel = viewModel()
                    AppNavHost(vm = vm)
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val QUIZ = "quiz"
    const val RESULT = "result"
}

@Composable
private fun AppNavHost(
    vm: QuizViewModel,
    navController: NavHostController = rememberNavController(),
) {
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                val home by vm.homeState.collectAsState()
                HomeScreen(
                    state = home,
                    onStart = { category, mode ->
                        vm.startSession(category, mode)
                        navController.navigate(Routes.QUIZ)
                    },
                    onClearProgress = vm::clearProgress,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            composable(Routes.QUIZ) {
                val quiz by vm.quizState.collectAsState()

                // セッション終了時に結果画面へ遷移
                LaunchedEffect(quiz.finished) {
                    if (quiz.finished) {
                        navController.navigate(Routes.RESULT) {
                            popUpTo(Routes.QUIZ) { inclusive = true }
                        }
                    }
                }

                QuizScreen(
                    state = quiz,
                    onSelect = vm::selectChoice,
                    onSubmit = vm::submit,
                    onNext = vm::next,
                    onQuit = {
                        vm.resetSession()
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            composable(Routes.RESULT) {
                val quiz by vm.quizState.collectAsState()
                ResultScreen(
                    answered = quiz.sessionAnswered,
                    correct = quiz.sessionCorrect,
                    onRetry = {
                        vm.startSession(category = null, mode = QuizMode.INCORRECT_ONLY)
                        navController.navigate(Routes.QUIZ) {
                            popUpTo(Routes.RESULT) { inclusive = true }
                        }
                    },
                    onHome = {
                        vm.resetSession()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
