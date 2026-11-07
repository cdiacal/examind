package com.cdiacal.examind.nucleo

/** Calificación universitaria española a partir de la nota sobre 10. */
enum class Calificacion(val texto: String) {
    SUSPENSO("Suspenso"), APROBADO("Aprobado"), NOTABLE("Notable"), SOBRESALIENTE("Sobresaliente");

    val aprobado: Boolean get() = this != SUSPENSO

    companion object {
        /** Se redondea a una décima antes de clasificar, como se muestra la nota (4,96 se ve 5 y es Aprobado). */
        fun de(nota: Double): Calificacion {
            val n = Math.round(nota * 10) / 10.0
            return when {
                n >= 9.0 -> SOBRESALIENTE
                n >= 7.0 -> NOTABLE
                n >= 5.0 -> APROBADO
                else -> SUSPENSO
            }
        }
    }
}

/** Los datos de la cabecera de inicio. [mediaAciertos] es null si aún no hay ningún test. */
data class ResumenInicio(
    val temarios: Int,
    val preguntas: Int,
    val tests: Int,
    val mediaAciertos: Double?
) {
    companion object {
        /** [ultimosTests]: cuántos de los tests más recientes cuentan para la media (los de hace meses pesan poco). */
        fun calcular(temarios: Int, preguntas: Int, resultadosRecientesPrimero: List<ResultadoTest>, ultimosTests: Int = 20) =
            ResumenInicio(temarios, preguntas, resultadosRecientesPrimero.size, Puntuacion.mediaAciertos(resultadosRecientesPrimero.take(ultimosTests)))
    }
}
