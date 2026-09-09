package `in`.acstechnologies.nutritrainerai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform