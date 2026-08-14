package co.ratmo.anreal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform