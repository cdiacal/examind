package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogoModelosTest {
    @Test fun ramNominal() {
        assertEquals(8, CatalogoModelos.ramNominalGb(7_300_000_000))
        assertEquals(6, CatalogoModelos.ramNominalGb(5_600_000_000))
        assertEquals(12, CatalogoModelos.ramNominalGb(11_200_000_000))
        assertEquals(4, CatalogoModelos.ramNominalGb(3_700_000_000))
    }

    @Test fun recomendado() {
        assertEquals(CatalogoModelos.COMPLETO, CatalogoModelos.recomendado(8))
        assertEquals(CatalogoModelos.COMPLETO, CatalogoModelos.recomendado(12))
        assertEquals(CatalogoModelos.LIGERO, CatalogoModelos.recomendado(6))
        assertNull(CatalogoModelos.recomendado(4))
    }

    @Test fun buscarPorArchivoEId() {
        assertEquals(CatalogoModelos.LIGERO, CatalogoModelos.porArchivo("qwen2.5-1.5b-instruct_multi-prefill-seq_q8_ekv4096.litertlm"))
        assertNull(CatalogoModelos.porArchivo("otro.litertlm"))
        assertEquals(CatalogoModelos.COMPLETO, CatalogoModelos.porId("gemma-4-e2b"))
    }
}
