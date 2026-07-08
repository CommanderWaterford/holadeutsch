package com.holadeutsch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.holadeutsch.app.HolaDeutschApp
import com.holadeutsch.app.ui.browse.WordListScreen
import com.holadeutsch.app.ui.home.HomeScreen
import com.holadeutsch.app.ui.onboarding.OnboardingScreen
import com.holadeutsch.app.ui.progress.ProgressScreen
import com.holadeutsch.app.ui.quiz.QuizScreen
import com.holadeutsch.app.ui.result.ResultScreen
import com.holadeutsch.app.ui.vocab.VocabListsScreen
import kotlinx.coroutines.flow.first

object Routes {
    const val ONBOARDING = "onboarding"
    const val VOCAB = "vocab"
    const val HOME = "home"
    const val QUIZ = "quiz?wordIds={wordIds}"

    /** Quiz over specific word ids (e.g. re-practice mistakes); "-" means a normal session. */
    fun quiz(wordIds: String = "-") = "quiz?wordIds=$wordIds"
    const val BROWSE = "browse"
    const val PROGRESS = "progress"
    const val RESULT = "result/{correct}/{total}/{xp}/{goalMet}/{streak}/{perfect}/{wrongIds}"

    fun result(
        correct: Int,
        total: Int,
        xp: Int,
        goalMet: Boolean,
        streak: Int,
        perfect: Boolean,
        wrongIds: List<Int>
    ): String {
        val ids = if (wrongIds.isEmpty()) "-" else wrongIds.joinToString(",")
        return "result/$correct/$total/$xp/$goalMet/$streak/$perfect/$ids"
    }
}

@Composable
fun HolaNavHost(navController: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as HolaDeutschApp
    // Hold the NavHost until we know whether first-run setup has been completed.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val done = app.container.statsRepository.stats.first().onboardingDone
        startDestination = if (done) Routes.HOME else Routes.ONBOARDING
    }
    val start = startDestination ?: return

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onStartQuiz = { navController.navigate(Routes.quiz()) },
                onBrowse = { navController.navigate(Routes.BROWSE) },
                onProgress = { navController.navigate(Routes.PROGRESS) },
                onVocabLists = { navController.navigate(Routes.VOCAB) }
            )
        }

        composable(Routes.VOCAB) {
            VocabListsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("wordIds") {
                    type = NavType.StringType
                    defaultValue = "-"
                }
            )
        ) {
            QuizScreen(
                onFinished = { outcome, correct, total, wrongIds ->
                    navController.navigate(
                        Routes.result(
                            correct = correct,
                            total = total,
                            xp = outcome.xpAwarded,
                            goalMet = outcome.goalJustMet,
                            streak = outcome.streak,
                            perfect = outcome.perfect,
                            wrongIds = wrongIds
                        )
                    ) {
                        popUpTo(Routes.HOME)
                    }
                },
                onExit = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("correct") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType },
                navArgument("xp") { type = NavType.IntType },
                navArgument("goalMet") { type = NavType.BoolType },
                navArgument("streak") { type = NavType.IntType },
                navArgument("perfect") { type = NavType.BoolType },
                navArgument("wrongIds") { type = NavType.StringType }
            )
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            val wrongIds = args.getString("wrongIds") ?: "-"
            ResultScreen(
                correct = args.getInt("correct"),
                total = args.getInt("total"),
                xp = args.getInt("xp"),
                goalMet = args.getBoolean("goalMet"),
                streak = args.getInt("streak"),
                perfect = args.getBoolean("perfect"),
                onPlayAgain = {
                    navController.navigate(Routes.quiz()) { popUpTo(Routes.HOME) }
                },
                onPracticeMistakes = {
                    navController.navigate(Routes.quiz(wrongIds)) { popUpTo(Routes.HOME) }
                },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }

        composable(Routes.BROWSE) {
            WordListScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PROGRESS) {
            ProgressScreen(onBack = { navController.popBackStack() })
        }
    }
}
