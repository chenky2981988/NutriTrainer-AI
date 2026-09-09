package `in`.acstechnologies.nutritrainerai.data.measurement

import `in`.acstechnologies.nutritrainerai.data.db.MeasurementEntity
import `in`.acstechnologies.nutritrainerai.domain.model.Measurement
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementSource

internal fun MeasurementEntity.toDomain(): Measurement =
    Measurement(
        id = id,
        takenEpochDay = takenEpochDay,
        weightKg = weightKg,
        waistCm = waistCm,
        conditionsNote = conditionsNote,
        source = MeasurementSource.valueOf(source),
        createdAtEpochMillis = createdAtEpochMillis,
    )
