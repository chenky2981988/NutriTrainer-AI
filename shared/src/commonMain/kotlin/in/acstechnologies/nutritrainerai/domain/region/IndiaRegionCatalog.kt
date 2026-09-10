package `in`.acstechnologies.nutritrainerai.domain.region

/** Zones used only to group the picker (PRD §4.1.1); not a dietary claim. */
enum class IndiaZone(val label: String) {
    NORTH("North"),
    WEST_AND_CENTRAL("West and Central"),
    SOUTH("South"),
    EAST("East"),
    NORTHEAST("Northeast"),
}

enum class RegionKind { STATE, UNION_TERRITORY }

/**
 * One state or union territory. [code] is the stable id stored on the profile
 * (ISO 3166-2:IN); [name] and [aliases] are match text only — real display
 * labels come from localization packs (PRD §4.1.2).
 */
data class RegionEntry(
    val code: String,
    val name: String,
    val kind: RegionKind,
    val zone: IndiaZone,
    val aliases: List<String> = emptyList(),
)

/**
 * The full offline index of **all 28 states and 8 union territories**
 * (PRD §4.1.1 "must cover all Indian states and union territories rather than
 * treating a few large cuisines as national defaults"). Bundled in the app;
 * no network needed. Sub-regional / food-tradition packs are layered on later.
 */
object IndiaRegionCatalog {

    val entries: List<RegionEntry> = listOf(
        // --- North ---
        RegionEntry("IN-CH", "Chandigarh", RegionKind.UNION_TERRITORY, IndiaZone.NORTH),
        RegionEntry("IN-DL", "Delhi (NCT)", RegionKind.UNION_TERRITORY, IndiaZone.NORTH, listOf("NCT", "New Delhi")),
        RegionEntry("IN-HR", "Haryana", RegionKind.STATE, IndiaZone.NORTH),
        RegionEntry("IN-HP", "Himachal Pradesh", RegionKind.STATE, IndiaZone.NORTH, listOf("Himachal")),
        RegionEntry("IN-JK", "Jammu and Kashmir", RegionKind.UNION_TERRITORY, IndiaZone.NORTH, listOf("J&K", "Kashmir")),
        RegionEntry("IN-LA", "Ladakh", RegionKind.UNION_TERRITORY, IndiaZone.NORTH),
        RegionEntry("IN-PB", "Punjab", RegionKind.STATE, IndiaZone.NORTH),
        RegionEntry("IN-RJ", "Rajasthan", RegionKind.STATE, IndiaZone.NORTH),
        RegionEntry("IN-UP", "Uttar Pradesh", RegionKind.STATE, IndiaZone.NORTH, listOf("UP")),
        RegionEntry("IN-UK", "Uttarakhand", RegionKind.STATE, IndiaZone.NORTH, listOf("Uttaranchal")),

        // --- West and Central ---
        RegionEntry("IN-CT", "Chhattisgarh", RegionKind.STATE, IndiaZone.WEST_AND_CENTRAL),
        RegionEntry(
            "IN-DH", "Dadra and Nagar Haveli and Daman and Diu",
            RegionKind.UNION_TERRITORY, IndiaZone.WEST_AND_CENTRAL, listOf("Daman", "Diu", "DNH"),
        ),
        RegionEntry("IN-GA", "Goa", RegionKind.STATE, IndiaZone.WEST_AND_CENTRAL),
        RegionEntry("IN-GJ", "Gujarat", RegionKind.STATE, IndiaZone.WEST_AND_CENTRAL),
        RegionEntry("IN-MP", "Madhya Pradesh", RegionKind.STATE, IndiaZone.WEST_AND_CENTRAL, listOf("MP")),
        RegionEntry("IN-MH", "Maharashtra", RegionKind.STATE, IndiaZone.WEST_AND_CENTRAL),

        // --- South ---
        RegionEntry(
            "IN-AN", "Andaman and Nicobar Islands",
            RegionKind.UNION_TERRITORY, IndiaZone.SOUTH, listOf("Andaman", "Nicobar"),
        ),
        RegionEntry("IN-AP", "Andhra Pradesh", RegionKind.STATE, IndiaZone.SOUTH, listOf("AP")),
        RegionEntry("IN-KA", "Karnataka", RegionKind.STATE, IndiaZone.SOUTH),
        RegionEntry("IN-KL", "Kerala", RegionKind.STATE, IndiaZone.SOUTH),
        RegionEntry("IN-LD", "Lakshadweep", RegionKind.UNION_TERRITORY, IndiaZone.SOUTH),
        RegionEntry("IN-PY", "Puducherry", RegionKind.UNION_TERRITORY, IndiaZone.SOUTH, listOf("Pondicherry")),
        RegionEntry("IN-TN", "Tamil Nadu", RegionKind.STATE, IndiaZone.SOUTH, listOf("TN")),
        RegionEntry("IN-TG", "Telangana", RegionKind.STATE, IndiaZone.SOUTH),

        // --- East ---
        RegionEntry("IN-BR", "Bihar", RegionKind.STATE, IndiaZone.EAST),
        RegionEntry("IN-JH", "Jharkhand", RegionKind.STATE, IndiaZone.EAST),
        RegionEntry("IN-OR", "Odisha", RegionKind.STATE, IndiaZone.EAST, listOf("Orissa")),
        RegionEntry("IN-WB", "West Bengal", RegionKind.STATE, IndiaZone.EAST, listOf("Bengal")),

        // --- Northeast ---
        RegionEntry("IN-AR", "Arunachal Pradesh", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-AS", "Assam", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-MN", "Manipur", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-ML", "Meghalaya", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-MZ", "Mizoram", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-NL", "Nagaland", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-SK", "Sikkim", RegionKind.STATE, IndiaZone.NORTHEAST),
        RegionEntry("IN-TR", "Tripura", RegionKind.STATE, IndiaZone.NORTHEAST),
    )

    private val byCode: Map<String, RegionEntry> = entries.associateBy { it.code }

    val states: List<RegionEntry> get() = entries.filter { it.kind == RegionKind.STATE }
    val unionTerritories: List<RegionEntry> get() = entries.filter { it.kind == RegionKind.UNION_TERRITORY }

    fun byCode(code: String): RegionEntry? = byCode[code]

    /** Entries grouped by zone, in the PRD's zone order. */
    fun grouped(): Map<IndiaZone, List<RegionEntry>> =
        IndiaZone.entries.associateWith { zone -> entries.filter { it.zone == zone } }

    /**
     * Case-insensitive match on name, aliases and code. A blank query returns
     * everything (the picker shows the full grouped list — nothing preselected,
     * PRD §4.1.1).
     */
    fun search(query: String): List<RegionEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries
        return entries.filter { entry ->
            entry.name.lowercase().contains(q) ||
                entry.code.lowercase().contains(q) ||
                entry.aliases.any { it.lowercase().contains(q) }
        }
    }
}
