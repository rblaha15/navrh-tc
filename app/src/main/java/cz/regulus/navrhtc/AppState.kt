package cz.regulus.navrhtc

import cz.regulus.navrhtc.Jednotky.Kubik
import cz.regulus.navrhtc.Jednotky.Kubik.Companion.m3
import cz.regulus.navrhtc.Jednotky.Litr
import cz.regulus.navrhtc.Jednotky.Litr.Companion.l
import cz.regulus.navrhtc.Jednotky.MJ
import cz.regulus.navrhtc.Jednotky.MJ.Companion.MJ
import cz.regulus.navrhtc.Jednotky.MJnakg
import cz.regulus.navrhtc.Jednotky.MJnakg.Companion.MJnakg
import cz.regulus.navrhtc.Jednotky.MWh
import cz.regulus.navrhtc.Jednotky.MWh.Companion.MWh
import cz.regulus.navrhtc.Jednotky.Procento
import cz.regulus.navrhtc.Jednotky.Procento.Companion.procent
import cz.regulus.navrhtc.Jednotky.kW
import cz.regulus.navrhtc.Jednotky.kW.Companion.kW
import cz.regulus.navrhtc.Jednotky.kWh
import cz.regulus.navrhtc.Jednotky.kWh.Companion.kWh
import cz.regulus.navrhtc.Jednotky.kg
import cz.regulus.navrhtc.Jednotky.kg.Companion.kg
import cz.regulus.navrhtc.Jednotky.kgnam3
import cz.regulus.navrhtc.Jednotky.kgnam3.Companion.kgnam3
import cz.regulus.navrhtc.Jednotky.q
import cz.regulus.navrhtc.Jednotky.q.Companion.q

data class AppState(
    val cerpadla: TepelnaCerpadla = TepelnaCerpadla(emptyList()),
    val typVypoctu: TypVypoctu = TypVypoctu.Nevybrano,

    val druhTC: TepelnaCerpadla.DruhTC = TepelnaCerpadla.DruhTC("", emptyList()),

    val typTC: TepelnaCerpadla.DruhTC.TypTC = TepelnaCerpadla.DruhTC.TypTC("", emptyList()),
) {

    init {
        println(this)
    }

    val mamCerpadla = cerpadla.druhy.isNotEmpty()

    val druhyTC = cerpadla.druhy

    val typyTC = druhTC.typy

    val doporuceneTC get() =
        if (typTC.cerpadla.isEmpty() || druhTC.typy.isEmpty() || cerpadla.druhy.isEmpty()) ""
        else typTC.cerpadla.find {
            when (typVypoctu.typ) {
                TypVypoctu.TypTypuVypoctu.PotrebnaE -> typVypoctu.potrebaEnergie.toInt() in it.tv
                TypVypoctu.TypTypuVypoctu.ZtracenyQ -> typVypoctu.tepelnaZtrata.toInt() in it.ztraty
            }
        }?.jmeno ?: "Nutno volit kaskádu – kontaktujte nás"

    sealed interface TypVypoctu {
        val potrebaEnergie: kWh
            get() = 0.kWh
        val tepelnaZtrata: kW
            get() = 0.kW
        val nazev: String
        val typ: TypTypuVypoctu

        enum class TypTypuVypoctu {
            PotrebnaE,
            ZtracenyQ,
        }

        object Nevybrano : TypVypoctu {
            override val nazev = ""
            override val typ = TypTypuVypoctu.PotrebnaE
        }

        data class Plyn(
            val spotrebaString: String = "",
            val ucinnostKotleString: String = "",
            val varimePlynem: Boolean = true,
        ) : TypVypoctu {
            private val spotreba: MWh = spotrebaString.toDoubleOrNull()?.MWh ?: 0.MWh
            private val ucinnostKotle: Procento = ucinnostKotleString.toIntOrNull()?.procent ?: 0.procent

            override val nazev = "Zemní plyn"
            override val typ = TypTypuVypoctu.PotrebnaE
            override val potrebaEnergie = (spotreba - varimePlynem.toInt().MWh).tokWh().nezapor() * ucinnostKotle
        }

        data class LTO(
            val spotrebaString: String = "",
            val ucinnostKotleString: String = "85",
        ) : TypVypoctu {
            private val spotreba: Litr = spotrebaString.toIntOrNull()?.l ?: 0.l
            private val ucinnostKotle: Procento = ucinnostKotleString.toIntOrNull()?.procent ?: 0.procent

            override val nazev = "Lehký topný olej"
            override val typ = TypTypuVypoctu.PotrebnaE
            override val potrebaEnergie = (spotreba.value * 42.5 * 0.9).MJ.tokWh() * ucinnostKotle
        }

        data class Elektrina(
            val spotrebaNTstring: String = "",
            val spotrebaVTstring: String = "",
            /*val typSazby: TypSazby = TypSazby.A,*/
        ) : TypVypoctu {
            private val spotrebaNT: MWh = spotrebaNTstring.toDoubleOrNull()?.MWh ?: 0.MWh
            private val spotrebaVT: MWh = spotrebaVTstring.toDoubleOrNull()?.MWh ?: 0.MWh

            /*enum class TypSazby(val nazev: String, val dobaTrvaniNT: Int) {
                A("D25d nebo D26d", 8),
                B("D35d", 16),
                C("D45d nebo D57d", 20),
                D("D55d nebo D56d", 22),
            }*/

            override val nazev = "Elektřina"
            override val typ = TypTypuVypoctu.PotrebnaE
            override val potrebaEnergie = (spotrebaNT.tokWh() + spotrebaVT.tokWh() - 3000.kWh).nezapor() /*/ (24.0 - typSazby.dobaTrvaniNT) * typSazby.dobaTrvaniNT.toDouble()*/
        }

        data class PENB(
            val potrebaENaVytapeniString: String = "",
            val potrebaENaTVString: String = "",
        ) : TypVypoctu {
            private val potrebaENaVytapeni: kWh = potrebaENaVytapeniString.toIntOrNull()?.kWh ?: 0.kWh
            private val potrebaENaTV: kWh = potrebaENaTVString.toIntOrNull()?.kWh ?: 0.kWh

            override val nazev = "PENB"
            override val typ = TypTypuVypoctu.PotrebnaE
            override val potrebaEnergie = potrebaENaVytapeni / 80.procent + potrebaENaTV / 90.procent
        }

        data class TZ(
            val tepelnaZtrataString: String = "",
        ) : TypVypoctu {
            override val tepelnaZtrata: kW = tepelnaZtrataString.toDoubleOrNull()?.kW ?: 0.kW
            override val nazev = "Tepelná ztráta"
            override val typ = TypTypuVypoctu.ZtracenyQ
        }

        data class Tuhy(
            val uhliString: String = "",
            val typUhli: TypUhli = TypUhli.Hnede,
            val drevoString: String = "",
            val typDreva: TypDreva = TypDreva.Mekke,
            val ostatniString: String = "",
            val typOstatniho: TypOstatniho = TypOstatniho.Pilina,

            val rezimVytapeni: RezimVytapeni = RezimVytapeni.PrerusovanyRadiatory,

            val ucinnostKotleString: String = "",

            val aplikace: TypAplikaceTC = TypAplikaceTC.ProTVBoiler,
        ) : TypVypoctu {
            private val uhli: q = uhliString.toIntOrNull()?.q ?: 0.q
            private val drevo: Kubik = drevoString.toIntOrNull()?.m3 ?: 0.m3
            private val ostatni: kg = ostatniString.toIntOrNull()?.kg ?: 0.kg
            private val ucinnostKotle: Procento = ucinnostKotleString.toIntOrNull()?.procent ?: 0.procent

            override val nazev = "Tuhá paliva"
            override val typ = TypTypuVypoctu.PotrebnaE

            enum class TypUhli(val nazev: String, val vyhrevnost: MJnakg) {
                Hnede(nazev = "Hnědé", vyhrevnost = 18.0.MJnakg),
                Cerne(nazev = "Černé (koks)", vyhrevnost = 18.0.MJnakg)
            }

            enum class TypDreva(val nazev: String, val vyhrevnost: MJnakg, val sypnaHmotnost: kgnam3) {
                Mekke(nazev = "Měkké", vyhrevnost = 14.5.MJnakg, sypnaHmotnost = 320.kgnam3),
                Tvrde(nazev = "Tvrdé", vyhrevnost = 14.0.MJnakg, sypnaHmotnost = 500.kgnam3)
            }

            enum class TypOstatniho(val nazev: String, val vyhrevnost: MJnakg) {
                Pilina(nazev = "Pilina", vyhrevnost = 12.2.MJnakg),
                Stepka(nazev = "Štěpka", vyhrevnost = 12.2.MJnakg),
                Byliny(nazev = "Bylinná peleta", vyhrevnost = 16.1.MJnakg),
                Peleta(nazev = "Dřevěná peleta", vyhrevnost = 17.1.MJnakg),
                Briketa(nazev = "Dřevěná briketa", vyhrevnost = 17.1.MJnakg)
            }

            enum class RezimVytapeni(val nazev: String, val ucinnost: Double) {
                PrerusovanyRadiatory(nazev = "Radiátory s přerušovaným provozem (ráno chladno - večer přetopeno) - tuhá paliva bez akumulace", ucinnost = 1.15),
                NepretrzityRadiatory(nazev = "Radiátory s nepřetržitým provozem - tuhá paliva s akumulační nádrží", ucinnost = 1.08),
                Podlahovka(nazev = "Podlahové vytápění", ucinnost = 1.0)
            }

            private val ucinnostPaliva: MJ
                get() = (uhli.toKg() * typUhli.vyhrevnost + drevo.toKg(typDreva.sypnaHmotnost) * typDreva.vyhrevnost + ostatni * typOstatniho.vyhrevnost) * rezimVytapeni.ucinnost

            val potrebaTepla = (ucinnostPaliva.tokWh() * ucinnostKotle)

            enum class TypAplikaceTC(val nazev: String, val bonusovaPotrebaEnergie: kWh) {
                ProVytapeni(nazev = "Tepelné čerpadlo pouze pro vytápění", bonusovaPotrebaEnergie = 0.kWh),
                ProTVPresLeto(nazev = "Tepelné čerpadlo pro vytápění a TV (původní kotel v zimě připravuje TV)", bonusovaPotrebaEnergie = 1825.kWh),
                ProTVBoiler(nazev = "Tepelné čerpadlo pro vytápění a TV (původně elektrický bojler)", bonusovaPotrebaEnergie = 3650.kWh),
            }

            override val potrebaEnergie = if (potrebaTepla == 0.kWh) 0.kWh else potrebaTepla + aplikace.bonusovaPotrebaEnergie
        }

        companion object {
            val typy = listOf(Tuhy(), Elektrina(), LTO(), Plyn(), PENB(), TZ())
        }
    }
}
