package cz.regulus.navrhtc

import kotlin.math.roundToInt


interface Jednotky {

    @JvmInline
    value class kWh(val value: Double) : Jednotky {
        fun nezapor() = if (value < 0) kWh(0.0) else this
        fun toMWh() = MWh(value / 1000.0)
        fun toInt() = value.roundToInt()
        operator fun plus(other: kWh) = kWh(value + other.value)
        operator fun minus(other: kWh) = kWh(value - other.value)
        operator fun times(procento: Procento) = kWh(value * procento.toDouble())
        operator fun times(other: Int) = kWh(value * other)
        operator fun times(other: Double) = kWh(value * other)
        operator fun div(procento: Procento) = kWh(value / procento.toDouble())
        operator fun div(other: Double) = kWh(value / other)
        override fun toString() = toInt().toString()

        companion object {
            val Int.kWh get() = kWh(toDouble())
            val Double.kWh get() = kWh(this)
        }
    }

    @JvmInline
    value class kW(val value: Double) : Jednotky {
        fun toInt() = value.roundToInt()
        override fun toString() = toInt().toString()
        companion object {
            val Int.kW get() = kW(toDouble())
            val Double.kW get() = kW(this)
        }
    }

    @JvmInline
    value class MWh(val value: Double) : Jednotky {
        fun tokWh() = kWh(value * 1000.0)
        fun toInt() = value.roundToInt()
        override fun toString() = if (toInt().toDouble() == value) toInt().nulaToString() else value.nulaToString()
        operator fun minus(other: MWh) = MWh(value - other.value)

        companion object {
            val Int.MWh get() = MWh(toDouble())
            val Double.MWh get() = MWh(this)
        }
    }

    @JvmInline
    value class MJ(val value: Double) : Jednotky {
        fun tokWh() = kWh(value / 3.6)
        fun toInt() = value.roundToInt()
        operator fun plus(other: MJ) = MJ(value + other.value)
        operator fun times(other: Double) = MJ(value * other)
        override fun toString() = toInt().toString()

        companion object {
            val Double.MJ get() = MJ(this)
            val Int.MJ get() = MJ(toDouble())
        }
    }

    @JvmInline
    value class Procento(val value: Int) : Jednotky {
        fun toDouble() = value / 100.0

        companion object {
            val Int.procent get() = Procento(this)
        }
    }

    @JvmInline
    value class q(val value: Int) : Jednotky {
        fun toKg() = kg(value * 100)

        companion object {
            val Int.q get() = q(this)
        }
    }

    @JvmInline
    value class kg(val value: Int) : Jednotky {
        operator fun times(other: MJnakg) = MJ(value * other.value)

        companion object {
            val Int.kg get() = kg(this)
        }
    }

    @JvmInline
    value class Kubik(val value: Int) : Jednotky {
        fun toKg(hustota: kgnam3) = kg(value * hustota.value)

        companion object {
            val Int.m3 get() = Kubik(this)
        }
    }

    @JvmInline
    value class Litr(val value: Int) : Jednotky {
        companion object {
            val Int.l get() = Litr(this)
        }
    }

    @JvmInline
    value class MJnakg(val value: Double) : Jednotky {

        companion object {
            val Double.MJnakg get() = MJnakg(this)
        }
    }

    @JvmInline
    value class kgnam3(val value: Int) : Jednotky {

        companion object {
            val Int.kgnam3 get() = kgnam3(this)
        }
    }
}
