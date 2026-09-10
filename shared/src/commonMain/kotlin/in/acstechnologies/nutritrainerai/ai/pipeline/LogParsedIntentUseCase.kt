package `in`.acstechnologies.nutritrainerai.ai.pipeline

import `in`.acstechnologies.nutritrainerai.ai.HouseholdDefault
import `in`.acstechnologies.nutritrainerai.ai.IntentKind
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntent
import `in`.acstechnologies.nutritrainerai.ai.ParsedItem
import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.resolve.FoodResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.QuantityResolver

/** What a [LogParsedIntentUseCase.log] call changed — feeds the Coach confirmation. */
data class LogOutcome(
    val created: List<MealItem> = emptyList(),
    val replaced: List<MealItem> = emptyList(),
    val removedIds: List<String> = emptyList(),
    /** Foods that could not be identified — stored as unconfirmed, out of totals. */
    val unresolvedFoods: List<String> = emptyList(),
    /** CORRECT/REMOVE targets that matched zero or many existing rows. */
    val unmatched: List<String> = emptyList(),
    val note: String? = null,
)

/**
 * Turns a validated [NutritionIntent] into rows in [MealLogRepository]: resolves
 * each food, converts the spoken quantity to grams, runs the **deterministic**
 * calculator, and persists. The AI never touches nutrition numbers here
 * (PRD "Primary decision").
 *
 * CONSUMED/PLAN add rows; CORRECT replaces the single matching row in place
 * (same id, revision + 1 — corrections replace, never duplicate, PRD §4.2);
 * REMOVE soft-deletes it; other kinds change nothing.
 */
class LogParsedIntentUseCase(
    private val mealLog: MealLogRepository,
    private val foods: FoodResolver,
    private val quantities: QuantityResolver,
    private val now: () -> Long,
    private val idFactory: (String) -> String = { it },
    private val calculationVersion: Int = 1,
) {

    suspend fun log(
        intent: NutritionIntent,
        dayEpochDay: Long,
        householdDefaults: List<HouseholdDefault> = emptyList(),
    ): LogOutcome = when (intent.kind) {
        IntentKind.CONSUMED, IntentKind.PLAN -> addItems(intent, dayEpochDay, householdDefaults)
        IntentKind.CORRECT -> correctItems(intent, dayEpochDay, householdDefaults)
        IntentKind.REMOVE -> removeItems(intent, dayEpochDay)
        IntentKind.REPEAT, IntentKind.ASK, IntentKind.SUGGEST, IntentKind.UNKNOWN ->
            LogOutcome(note = "no records changed for ${intent.kind}")
    }

    private suspend fun addItems(
        intent: NutritionIntent,
        day: Long,
        hh: List<HouseholdDefault>,
    ): LogOutcome {
        val status = if (intent.kind == IntentKind.PLAN) MealItemStatus.PLANNED else MealItemStatus.CONSUMED
        val createdAt = now()
        val created = mutableListOf<MealItem>()
        val unresolved = mutableListOf<String>()
        intent.items.forEachIndexed { index, parsed ->
            val item = buildItem(parsed, day, status, createdAt, index, hh, revision = 1, existingId = null)
            mealLog.upsert(item)
            created += item
            if (item.confidence == ConfidenceBand.UNRESOLVED) unresolved += parsed.foodName
        }
        return LogOutcome(created = created, unresolvedFoods = unresolved)
    }

    private suspend fun correctItems(
        intent: NutritionIntent,
        day: Long,
        hh: List<HouseholdDefault>,
    ): LogOutcome {
        val existing = mealLog.getDay(day)
        val replaced = mutableListOf<MealItem>()
        val unmatched = mutableListOf<String>()
        intent.items.forEachIndexed { index, parsed ->
            val key = canonical(parsed.foodName)
            val target = existing.filter { it.foodName.equals(key, ignoreCase = true) }.singleOrNull()
            if (target == null) {
                unmatched += parsed.foodName
            } else {
                val updated = buildItem(
                    parsed, day, target.status, target.createdAtEpochMillis, index, hh,
                    revision = target.revision + 1, existingId = target.id,
                )
                mealLog.upsert(updated)
                replaced += updated
            }
        }
        return LogOutcome(
            replaced = replaced,
            unmatched = unmatched,
            note = unmatched.takeIf { it.isNotEmpty() }?.let { "couldn't match ${it.joinToString()}" },
        )
    }

    private suspend fun removeItems(intent: NutritionIntent, day: Long): LogOutcome {
        val existing = mealLog.getDay(day)
        val at = now()
        val removed = mutableListOf<String>()
        val unmatched = mutableListOf<String>()
        intent.items.forEach { parsed ->
            val key = canonical(parsed.foodName)
            val target = existing.filter { it.foodName.equals(key, ignoreCase = true) }.singleOrNull()
            if (target == null) unmatched += parsed.foodName else {
                mealLog.softDelete(target.id, at)
                removed += target.id
            }
        }
        return LogOutcome(removedIds = removed, unmatched = unmatched)
    }

    private suspend fun buildItem(
        parsed: ParsedItem,
        day: Long,
        status: MealItemStatus,
        createdAt: Long,
        seedIndex: Int,
        hh: List<HouseholdDefault>,
        revision: Int,
        existingId: String?,
    ): MealItem {
        val resolved = foods.resolve(parsed.foodName)
        val overrides = hh
            .filter { it.foodName.equals(parsed.foodName, ignoreCase = true) }
            .associate { it.unitId to it.gramsPerUnit }
        val grams = quantities.toGrams(parsed.quantity?.amount, parsed.quantity?.unit, resolved, overrides)

        val hasNutrition = resolved != null && grams != null
        val nutrients = if (hasNutrition) {
            NutritionMath.ingredientNutrients(resolved.nutrientsPerBase, grams)
        } else {
            NutrientVector.ZERO
        }

        return MealItem(
            id = existingId ?: idFactory("$day:$createdAt:$seedIndex:${parsed.foodName}"),
            dayEpochDay = day,
            status = status,
            confirmed = hasNutrition,
            foodName = resolved?.canonicalName ?: parsed.foodName,
            quantityGrams = grams,
            nutrients = nutrients,
            confidence = if (hasNutrition) resolved.confidence else ConfidenceBand.UNRESOLVED,
            source = resolved?.source ?: SourceType.HEURISTIC_ESTIMATE,
            revision = revision,
            calculationVersion = calculationVersion,
            createdAtEpochMillis = createdAt,
        )
    }

    private suspend fun canonical(foodName: String): String =
        foods.resolve(foodName)?.canonicalName ?: foodName
}
