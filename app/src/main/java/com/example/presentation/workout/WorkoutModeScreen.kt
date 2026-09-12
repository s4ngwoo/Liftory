package com.example.presentation.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Stateless 3-page workout mode shell (N06).
 * Pager index maps to [WorkoutModePage], never to exercise index.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutModeScreen(
    state: WorkoutModeUiState,
    onSelectPage: (WorkoutModePage) -> Unit,
    onPrimaryAction: () -> Unit,
    onPeekPlanRow: (String) -> Unit,
    onToggleMoreMenu: () -> Unit = {},
    onSubstituteExercise: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pages = WorkoutModePage.entries
    val pagerState = rememberPagerState(
        initialPage = state.displayPage.ordinal,
        pageCount = { pages.size }
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.displayPage) {
        val target = state.displayPage.ordinal
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val page = pages[pagerState.settledPage]
        if (page != state.displayPage) {
            onSelectPage(page)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("workout_mode_root")
    ) {
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("error_banner")
            )
        }
        ScrollableTabRow(selectedTabIndex = state.displayPage.ordinal) {
            pages.forEachIndexed { index, page ->
                Tab(
                    selected = state.displayPage == page,
                    onClick = {
                        onSelectPage(page)
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = when (page) {
                                WorkoutModePage.TIMER -> "타이머"
                                WorkoutModePage.CURRENT_EXERCISE -> "이번 종목"
                                WorkoutModePage.TODAY_PLAN -> "오늘 계획"
                            }
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "workout_page_${page.name}"
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).testTag("workout_mode_pager"),
            contentPadding = PaddingValues(0.dp),
            userScrollEnabled = true
        ) { pageIndex ->
            when (pages[pageIndex]) {
                WorkoutModePage.TIMER -> TimerPage(
                    state = state,
                    onPrimaryAction = onPrimaryAction,
                    onToggleMoreMenu = onToggleMoreMenu,
                    onSubstituteExercise = onSubstituteExercise
                )
                WorkoutModePage.CURRENT_EXERCISE -> CurrentExercisePage(state = state)
                WorkoutModePage.TODAY_PLAN -> TodayPlanPage(state = state, onPeekPlanRow = onPeekPlanRow)
            }
        }
    }
}

@Composable
private fun TimerPage(
    state: WorkoutModeUiState,
    onPrimaryAction: () -> Unit,
    onToggleMoreMenu: () -> Unit,
    onSubstituteExercise: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("page_timer")
    ) {
        val snapshot = state.timerSnapshot
        Text(
            text = when {
                (snapshot?.restRemainingSeconds ?: 0) > 0 ->
                    "휴식 ${snapshot?.restRemainingSeconds}s"
                (snapshot?.restOvertimeSeconds ?: 0) > 0 ->
                    "초과 ${snapshot?.restOvertimeSeconds}s"
                else -> "수행 ${(snapshot?.performedSeconds ?: 0)}s"
            },
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { contentDescription = "primary_timer" }
        )
        Text(
            text = "전체 ${(snapshot?.sessionElapsedSeconds ?: 0)}s",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .testTag("primary_cta")
                .semantics { contentDescription = "primary_action_${state.primaryActionLabel}" }
        ) {
            Text(state.primaryActionLabel)
        }
        Button(
            onClick = onToggleMoreMenu,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("more_menu")
        ) {
            Text(if (state.moreMenuOpen) "더보기 닫기" else "더보기")
        }
        if (state.moreMenuOpen) {
            Button(
                onClick = {
                    val next = (state.execution?.currentExerciseIndex ?: 0) + 1
                    onSubstituteExercise(next)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("more_substitute")
            ) {
                Text("종목 대체/전환")
            }
        }
    }
}

@Composable
private fun CurrentExercisePage(state: WorkoutModeUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("page_current_exercise")
    ) {
        val current = state.planRows.firstOrNull { it.isExecutionTarget }
        Text(
            text = current?.name ?: "종목 없음",
            style = MaterialTheme.typography.titleLarge
        )
        state.currentExerciseComparisons.forEach { row ->
            Text(
                text = "세트 ${row.setIndex + 1}: 계획=${row.planned} / 실제=${row.actual ?: "-"}",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TodayPlanPage(
    state: WorkoutModeUiState,
    onPeekPlanRow: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("page_today_plan")
    ) {
        state.planRows.forEach { row ->
            Button(
                onClick = { onPeekPlanRow(row.planItemId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("plan_row_${row.planItemId}")
            ) {
                Text("${row.name} · ${row.status}" + if (row.isExecutionTarget) " (실행 중)" else "")
            }
        }
    }
}
