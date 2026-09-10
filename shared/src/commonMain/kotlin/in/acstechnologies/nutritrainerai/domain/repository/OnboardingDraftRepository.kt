package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState

/**
 * Persists the in-progress onboarding answers locally. Every mutation is saved
 * so a killed app resumes exactly where it left off (PRD §4.1.2); [clear] runs
 * once onboarding completes.
 */
interface OnboardingDraftRepository {
    suspend fun load(): OnboardingState?
    suspend fun save(state: OnboardingState)
    suspend fun clear()
}
