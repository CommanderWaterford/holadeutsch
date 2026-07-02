package com.holadeutsch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.holadeutsch.app.ui.browse.WordListScreen
import com.holadeutsch.app.ui.home.HomeScreen
import com.holadeutsch.app.ui.progress.ProgressScreen
import com.holadeutsch.app.ui.quiz.QuizScreen
import com.holadeutsch.app.ui.result.ResultScreen

object Routes {
    const val HOME = "home"
    const val QUIZ = "quiz"
    const val BROWSE = "browse"
    const val PROGRESS = "progress"
    const val RESULT = "result/{correct}/{total}/{xp}/{goalMet}/{streak}/{perfect}"

    fun result(correct: Int, total: Int, xp: Int, goalMet: Boolean, streak: Int, perfect: Boolean) =
        "result/$correct/$total/$xp/$goalMet/$streak/$perfect"
}

@Composable
fun HolaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onStartQuiz = { navController.navigate(Routes.QUIZ) },
                onBrowse = { navController.navigate(Routes.BROWSE) },
                onProgress = { navController.navigate(Routes.PROGRESS) }
            )
        }

        composable(Routes.QUIZ) {
            QuizScreen(
                onFinished = { outcome, correct, total ->
                    navController.navigate(
                        Routes.result(
                            correct = correct,
                            total = total,
                            xp = outcome.xpAwarded,
                            goalMet = outcome.goalJustMet,
                            streak = outcome.streak,
                            perfect = outcome.perfect
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
                navArgument("perfect") { type = NavType.BoolType }
            )
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            ResultScreen(
                correct = args.getInt("correct"),
                total = args.getInt("total"),
                xp = args.getInt("xp"),
                goalMet = args.getBoolean("goalMet"),
                streak = args.getInt("streak"),
                perfect = args.getBoolean("perfect"),
                onPlayAgain = {
                    navController.navigate(Routes.QUIZ) { popUpTo(Routes.HOME) }
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
