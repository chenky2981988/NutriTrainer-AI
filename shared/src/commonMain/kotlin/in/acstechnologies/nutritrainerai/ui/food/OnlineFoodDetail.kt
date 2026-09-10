package `in`.acstechnologies.nutritrainerai.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.acstechnologies.nutritrainerai.domain.model.FoodSearchResult
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.ProductImage
import `in`.acstechnologies.nutritrainerai.domain.model.ProductImageKind
import `in`.acstechnologies.nutritrainerai.domain.repository.OnlineFoodSource
import org.koin.compose.koinInject
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_back
import nutritrainerai.shared.generated.resources.addfood_basis_100g
import nutritrainerai.shared.generated.resources.addfood_basis_100ml
import nutritrainerai.shared.generated.resources.addfood_basis_serving
import nutritrainerai.shared.generated.resources.addfood_detail_per
import nutritrainerai.shared.generated.resources.addfood_image_front
import nutritrainerai.shared.generated.resources.addfood_image_ingredients
import nutritrainerai.shared.generated.resources.addfood_image_nutrition
import nutritrainerai.shared.generated.resources.addfood_no_images
import nutritrainerai.shared.generated.resources.addfood_serving_line
import nutritrainerai.shared.generated.resources.addfood_use_this
import nutritrainerai.shared.generated.resources.addfood_via_off
import nutritrainerai.shared.generated.resources.nutrient_carbs
import nutritrainerai.shared.generated.resources.nutrient_fat
import nutritrainerai.shared.generated.resources.nutrient_fibre
import nutritrainerai.shared.generated.resources.nutrient_energy
import nutritrainerai.shared.generated.resources.nutrient_energy_kcal
import nutritrainerai.shared.generated.resources.nutrient_grams
import nutritrainerai.shared.generated.resources.nutrient_protein
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Full detail for one online candidate (PRD §7): a swipeable photo gallery
 * (front / ingredients / nutrition panel) and the complete macro breakdown,
 * with an explicit "use this" step before it becomes a library [Food].
 */
@Composable
fun OnlineFoodDetail(
    result: FoodSearchResult,
    onUse: (FoodSearchResult) -> Unit,
    onBack: () -> Unit,
) {
    val online = koinInject<OnlineFoodSource>()
    // Search results only carry the front photo; the product endpoint also has
    // the ingredients and nutrition-panel photos. Fetch the full set on open.
    var images by remember(result.barcode) { mutableStateOf(result.imageUrls) }
    LaunchedEffect(result.barcode) {
        val code = result.barcode ?: return@LaunchedEffect
        val full = online.lookupByBarcode(code)?.imageUrls ?: return@LaunchedEffect
        if (full.size > images.size) images = full
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            listOfNotNull(result.brand, result.name).joinToString(" · "),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Gallery(images)

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(Res.string.addfood_detail_per, stringResource(basisLabel(result.basis))),
                    style = MaterialTheme.typography.labelMedium,
                )
                NutrientLine(
                    stringResource(Res.string.nutrient_energy),
                    stringResource(Res.string.nutrient_energy_kcal, result.nutrientsPerBase.energyKcal.roundToInt()),
                    bold = true,
                )
                NutrientLine(stringResource(Res.string.nutrient_protein), grams(result.nutrientsPerBase.proteinG))
                NutrientLine(stringResource(Res.string.nutrient_carbs), grams(result.nutrientsPerBase.carbohydrateG))
                NutrientLine(stringResource(Res.string.nutrient_fat), grams(result.nutrientsPerBase.fatG))
                NutrientLine(stringResource(Res.string.nutrient_fibre), grams(result.nutrientsPerBase.fibreG))
                result.servingGrams?.let {
                    Text(
                        stringResource(Res.string.addfood_serving_line, trimNum(it)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Text(stringResource(Res.string.addfood_via_off), style = MaterialTheme.typography.bodySmall)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.action_back))
            }
            Button(onClick = { onUse(result) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.addfood_use_this))
            }
        }
    }
}

@Composable
private fun Gallery(images: List<ProductImage>) {
    if (images.isEmpty()) {
        Text(
            stringResource(Res.string.addfood_no_images),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val pager = rememberPagerState(pageCount = { images.size })
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(state = pager) { page ->
            val image = images[page]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = image.url,
                    contentDescription = stringResource(imageKindLabel(image.kind)),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Text(
                    stringResource(imageKindLabel(image.kind)),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (images.size > 1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(images.size) { i ->
                    val on = i == pager.currentPage
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(if (on) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NutrientLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun grams(v: Double): String = stringResource(Res.string.nutrient_grams, trimNum(v))

private fun trimNum(v: Double): String {
    val rounded = (v * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

private fun basisLabel(basis: MeasurementBasis): StringResource = when (basis) {
    MeasurementBasis.PER_100_ML -> Res.string.addfood_basis_100ml
    MeasurementBasis.PER_SERVING -> Res.string.addfood_basis_serving
    else -> Res.string.addfood_basis_100g
}

private fun imageKindLabel(kind: ProductImageKind): StringResource = when (kind) {
    ProductImageKind.FRONT -> Res.string.addfood_image_front
    ProductImageKind.INGREDIENTS -> Res.string.addfood_image_ingredients
    ProductImageKind.NUTRITION -> Res.string.addfood_image_nutrition
}
