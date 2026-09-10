package `in`.acstechnologies.nutritrainerai.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.acstechnologies.nutritrainerai.domain.repository.OnboardingDraftRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI state holder for the onboarding flow (plan §2A).
 *
 * - [state] — the single immutable [OnboardingUiState] the screens render.
 * - [onIntent] — the only entry point; delegates to the pure [OnboardingReducer].
 * - [effects] — one-off signals (here, `Completed`).
 *
 * Every reduction persists the draft, so a killed app resumes where it left off
 * (PRD §4.1.2). On load it jumps to the first incomplete required step.
 */
class OnboardingViewModel(
    private val drafts: OnboardingDraftRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            drafts.load()?.let { saved ->
                _state.value = OnboardingUiState(
                    draft = saved,
                    step = OnboardingReducer.resumeStep(saved),
                )
            }
        }
    }

    fun onIntent(intent: OnboardingIntent) {
        val (nextState, effect) = OnboardingReducer.reduce(_state.value, intent)
        _state.value = nextState
        viewModelScope.launch {
            // The completed state stays persisted (completedAtEpochMillis set) so
            // the app knows onboarding is done. A real user-profile commit +
            // draft clear (PRD OB-10) lands with the profile entity.
            drafts.save(nextState.draft)
            if (effect != null) _effects.send(effect)
        }
    }
}
