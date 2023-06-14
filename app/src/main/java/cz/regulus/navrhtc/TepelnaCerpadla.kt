package cz.regulus.navrhtc

@JvmInline
value class TepelnaCerpadla(
    val druhy: List<DruhTC>,
) {

    data class DruhTC(
        val nazev: String,
        val typy: List<TypTC>,
    ) {

        data class TypTC(
            val nazev: String,
            val cerpadla: List<Cerpadlo>,
        ) {

            data class Cerpadlo(
                val jmeno: String,
                val tv: IntRange,
                val ztraty: IntRange,
            )
        }
    }
}
