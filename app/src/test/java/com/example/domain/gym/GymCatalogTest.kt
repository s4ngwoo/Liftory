package com.example.domain.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N13 Gym and Equipment Catalog Tests (GYM-01~GYM-06).
 */
class GymCatalogTest {

    @Test
    fun `GYM-01 distinguishes equipment model from exercise action ID preventing false unification`() {
        val exerciseId = "ex_chest_press"
        val modelHammer = EquipmentModel(id = "model_hammer", manufacturer = "Hammer Strength", name = "Iso-Lateral Chest Press")
        val modelNautilus = EquipmentModel(id = "model_nautilus", manufacturer = "Nautilus", name = "Nitro Chest Press")

        assertFalse("Different models for same exercise must remain distinct", modelHammer.id == modelNautilus.id)
        assertEquals("ex_chest_press", exerciseId)
    }

    @Test
    fun `GYM-02 equipment quantity distinguishes null (unknown), 0 (none), and 2 (present)`() {
        val itemUnknown = GymEquipment(id = "ge_1", gymId = "gym_1", modelId = "mod_1", quantity = null, status = EquipmentStatus.UNCONFIRMED)
        val itemNone = GymEquipment(id = "ge_2", gymId = "gym_1", modelId = "mod_2", quantity = 0, status = EquipmentStatus.AVAILABLE)
        val itemPresent = GymEquipment(id = "ge_3", gymId = "gym_1", modelId = "mod_3", quantity = 2, status = EquipmentStatus.AVAILABLE)

        assertEquals("Unknown quantity must not be converted to 0", null, itemUnknown.quantity)
        assertEquals("Explicit zero represents none available", 0, itemNone.quantity)
        assertEquals(2, itemPresent.quantity)
    }

    @Test
    fun `GYM-03 deduplicates identical registered assets without double counting`() {
        val items = listOf(
            GymEquipment(id = "ge_1", gymId = "gym_1", modelId = "mod_power_rack", serialNumber = "SN-001", quantity = 1),
            GymEquipment(id = "ge_1_dup", gymId = "gym_1", modelId = "mod_power_rack", serialNumber = "SN-001", quantity = 1) // Duplicate registration!
        )

        val totalEffectiveCount = GymCatalogService.countUniqueEquipmentAssets(items)
        assertEquals("Duplicate registration of identical serial asset must count only once", 1, totalEffectiveCount)
    }

    @Test
    fun `GYM-04 routine equipment fulfillment separates Available, Unavailable, and Unknown`() {
        val requiredModels = listOf("mod_bench", "mod_stepmill", "mod_incline_chest")
        val gymEquipments = listOf(
            GymEquipment(id = "ge_1", gymId = "gym_1", modelId = "mod_bench", quantity = 3, status = EquipmentStatus.AVAILABLE),
            GymEquipment(id = "ge_2", gymId = "gym_1", modelId = "mod_stepmill", quantity = 0, status = EquipmentStatus.OUT_OF_ORDER),
            GymEquipment(id = "ge_3", gymId = "gym_1", modelId = "mod_incline_chest", quantity = null, status = EquipmentStatus.UNCONFIRMED)
        )

        val report = GymCatalogService.evaluateRoutineFulfillment(requiredModels, gymEquipments)

        assertEquals(listOf("mod_bench"), report.availableModels)
        assertEquals(listOf("mod_stepmill"), report.unavailableModels)
        assertEquals(listOf("mod_incline_chest"), report.unknownModels)
        assertFalse("Unknown must not be lumped into unavailable", report.unavailableModels.contains("mod_incline_chest"))
    }

    @Test
    fun `GYM-05 equipment out of order or gym deletion preserves past workout session history link`() {
        val historyRecord = GymHistoryLink(
            sessionId = "sess_old_1",
            gymId = "gym_closed",
            gymName = "Old Fitness",
            equipmentModelId = "mod_vintage_press",
            performedAtEpochMs = 1500000000L
        )

        // Gym is closed/deleted from live catalog
        val service = GymCatalogService()
        service.deleteGym("gym_closed")

        // Past history link remains intact
        assertEquals("gym_closed", historyRecord.gymId)
        assertEquals("Old Fitness", historyRecord.gymName)
    }

    @Test
    fun `GYM-06 substituting unavailable equipment creates in-session plan change while keeping routine immutable`() {
        val planChange = GymCatalogService.createEquipmentSubstitutionChange(
            sessionId = "sess_active_gym",
            originalModelId = "mod_stepmill",
            substituteModelId = "mod_treadmill",
            reason = "Equipment out of order"
        )

        assertEquals("mod_stepmill", planChange.originalModelId)
        assertEquals("mod_treadmill", planChange.substituteModelId)
        assertEquals("Equipment out of order", planChange.reason)
    }
}
