package `in`.acstechnologies.nutritrainerai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingHost
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingReducer
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingUiState
import `in`.acstechnologies.nutritrainerai.ui.theme.NutriTheme
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OnboardingHostUiTest {

    /** Renders the real screen, drives clicks through the real reducer. */
    @Test
    fun foodPatternStep_selectingEnablesContinueAndAdvances() = runComposeUiTest {
        setContent {
            var state by remember { mutableStateOf(OnboardingUiState(step = OnboardingStep.FOOD_PATTERN)) }
            NutriTheme(darkTheme = false) {
                OnboardingHost(
                    state = state,
                    effects = emptyFlow(),
                    onIntent = { state = OnboardingReducer.reduce(state, it).state },
                    onCompleted = {},
                )
            }
        }

        onNodeWithText("Vegetarian").assertExists()
        onNodeWithText("Continue").assertIsNotEnabled()

        onNodeWithText("Vegetarian").performClick()
        onNodeWithText("Continue").assertIsEnabled().performClick()

        // advanced to OB-03
        onNodeWithText("Any specifics?").assertExists()
    }

    @Test
    fun requiredStep_hasNoSkipButton() = runComposeUiTest {
        setContent {
            NutriTheme(darkTheme = false) {
                OnboardingHost(
                    state = OnboardingUiState(step = OnboardingStep.FOOD_PATTERN),
                    effects = emptyFlow(),
                    onIntent = {},
                    onCompleted = {},
                )
            }
        }
        onNodeWithText("Skip for now").assertDoesNotExist()
        onNodeWithText("Step 2 of 10").assertExists()
    }

    @Test
    fun goalStep_collectsBodyProfileWithUnitAwareLabels() = runComposeUiTest {
        setContent {
            var state by remember { mutableStateOf(OnboardingUiState(step = OnboardingStep.GOAL_BODY)) }
            NutriTheme(darkTheme = false) {
                OnboardingHost(
                    state = state,
                    effects = emptyFlow(),
                    onIntent = { state = OnboardingReducer.reduce(state, it).state },
                    onCompleted = {},
                )
            }
        }

        onNodeWithText("Units").assertExists()
        onNodeWithText("Age band").assertExists()
        onNodeWithText("Height (cm)").assertExists()
        onNodeWithText("Weight (kg) — optional").assertExists()

        onNodeWithText("Imperial (lb, in)").performClick()
        onNodeWithText("Height (in)").assertExists()
        onNodeWithText("Weight (lb) — optional").assertExists()
    }
}
