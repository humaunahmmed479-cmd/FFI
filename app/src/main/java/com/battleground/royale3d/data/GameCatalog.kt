package com.battleground.royale3d.data

import com.battleground.royale3d.model.AppLanguage
import com.battleground.royale3d.model.ArmoryModelItem
import com.battleground.royale3d.model.VehicleSpec
import com.battleground.royale3d.model.VehicleType
import com.battleground.royale3d.model.WeaponSpec
import com.battleground.royale3d.model.WeaponType

object GameCatalog {

    const val MODEL_JERSEY = "models/all_freefire_rare_jersey_for_free_obj_file.glb"
    const val MODEL_PLANE_CABIN = "models/plane_cabin__freefire.glb"

    val weapons: Map<WeaponType, WeaponSpec> = mapOf(
        WeaponType.AWM to WeaponSpec(
            type = WeaponType.AWM,
            nameEn = "AWM Sniper",
            nameBn = "AWM স্নাইপার",
            categoryEn = "Bolt-Action Sniper",
            categoryBn = "স্নাইপার রাইফেল",
            modelAsset = "models/free_fire_awm_high_texture.glb",
            damage = 110,
            rangeMeters = 280f,
            fireRateSec = 1.25f,
            magSize = 5,
            recoilDegrees = 4.2f,
            reloadTimeSec = 3.2f,
            zoomFov = 18f,
            initialReserve = 20
        ),
        WeaponType.MP40_COBRA to WeaponSpec(
            type = WeaponType.MP40_COBRA,
            nameEn = "MP40 Cobra Evo",
            nameBn = "MP40 কোবরা ইভো",
            categoryEn = "Rapid Submachine Gun",
            categoryBn = "সাবমেশিনগান",
            modelAsset = "models/mp40_cobra_evo_gun.glb",
            damage = 23,
            rangeMeters = 70f,
            fireRateSec = 0.085f,
            magSize = 28,
            recoilDegrees = 1.3f,
            reloadTimeSec = 1.6f,
            zoomFov = 46f,
            initialReserve = 112
        ),
        WeaponType.FF_RIFLE to WeaponSpec(
            type = WeaponType.FF_RIFLE,
            nameEn = "FF Tactical Rifle",
            nameBn = "এফএফ অ্যাসল্ট রাইফেল",
            categoryEn = "Mid-Range Assault Rifle",
            categoryBn = "অ্যাসল্ট রাইফেল",
            modelAsset = "models/free_fire_gun.glb",
            damage = 34,
            rangeMeters = 150f,
            fireRateSec = 0.13f,
            magSize = 30,
            recoilDegrees = 1.8f,
            reloadTimeSec = 2.1f,
            zoomFov = 40f,
            initialReserve = 90
        ),
        WeaponType.BD_PISTOL to WeaponSpec(
            type = WeaponType.BD_PISTOL,
            nameEn = "BD Tactical Sidearm",
            nameBn = "বিডি ট্যাকটিক্যাল পিস্তল",
            categoryEn = "Backup Sidearm",
            categoryBn = "পিস্তল",
            modelAsset = "models/free_fire_gun.glb",
            damage = 28,
            rangeMeters = 65f,
            fireRateSec = 0.25f,
            magSize = 12,
            recoilDegrees = 1.1f,
            reloadTimeSec = 1.3f,
            zoomFov = 48f,
            initialReserve = 48
        )
    )

    val vehicles: Map<VehicleType, VehicleSpec> = mapOf(
        VehicleType.AUDI_R15 to VehicleSpec(
            type = VehicleType.AUDI_R15,
            nameEn = "2009 Audi R15 TDI",
            nameBn = "অডি R15 TDI রেসার",
            roleEn = "High-Speed Prototype",
            roleBn = "দ্রুত গতির রেসিং কার",
            modelAsset = "models/2009_audi_r15_tdi.glb",
            maxSpeedMs = 42f,
            maxHealth = 220,
            acceleration = 18f,
            turnRateRad = 1.9f,
            fuelBurnPerSec = 1.1f,
            collisionRadius = 2.6f
        ),
        VehicleType.BMW_I8 to VehicleSpec(
            type = VehicleType.BMW_I8,
            nameEn = "BMW i8 Coupe",
            nameBn = "বিএমডব্লিউ i8",
            roleEn = "Sport Interceptor",
            roleBn = "স্পোর্টস কার",
            modelAsset = "models/bmw_i8.glb",
            maxSpeedMs = 38f,
            maxHealth = 260,
            acceleration = 16f,
            turnRateRad = 2.1f,
            fuelBurnPerSec = 0.9f,
            collisionRadius = 2.5f
        ),
        VehicleType.AJF12_DVERGR to VehicleSpec(
            type = VehicleType.AJF12_DVERGR,
            nameEn = "AJF-12 Dvergr",
            nameBn = "AJF-12 হেভি আর্মার",
            roleEn = "Heavy Armored Carrier",
            roleBn = "ভারী সাঁজোয়া যান",
            modelAsset = "models/ajf-12_dvergr.glb",
            maxSpeedMs = 23f,
            maxHealth = 600,
            acceleration = 10f,
            turnRateRad = 1.35f,
            fuelBurnPerSec = 0.7f,
            collisionRadius = 3.2f
        )
    )

    val armoryItems: List<ArmoryModelItem> = listOf(
        ArmoryModelItem(
            id = "jersey",
            titleEn = "Rare Championship Jersey",
            titleBn = "রেয়ার চ্যাম্পিয়নশিপ জার্সি",
            subtitleEn = "Player Skin • 100 HP • Pure Fair Combat",
            subtitleBn = "প্লেয়ার স্কিন • ১০০ স্বাস্থ্য • কোনো স্পেশাল পাওয়ার নেই",
            modelAsset = MODEL_JERSEY,
            category = "SKIN"
        ),
        ArmoryModelItem(
            id = "awm",
            titleEn = weapons.getValue(WeaponType.AWM).nameEn,
            titleBn = weapons.getValue(WeaponType.AWM).nameBn,
            subtitleEn = "High Damage • 8x Scope • Slow Reload",
            subtitleBn = "দূরপাল্লার স্নাইপার • বড় ক্ষতি • ধীর রিলোড",
            modelAsset = weapons.getValue(WeaponType.AWM).modelAsset,
            category = "WEAPON",
            weaponSpec = weapons.getValue(WeaponType.AWM)
        ),
        ArmoryModelItem(
            id = "mp40",
            titleEn = weapons.getValue(WeaponType.MP40_COBRA).nameEn,
            titleBn = weapons.getValue(WeaponType.MP40_COBRA).nameBn,
            subtitleEn = "Extreme Fire Rate • Close Combat SMG",
            subtitleBn = "কাছের লড়াইয়ের জন্য দ্রুত ফায়ার",
            modelAsset = weapons.getValue(WeaponType.MP40_COBRA).modelAsset,
            category = "WEAPON",
            weaponSpec = weapons.getValue(WeaponType.MP40_COBRA)
        ),
        ArmoryModelItem(
            id = "ff_gun",
            titleEn = weapons.getValue(WeaponType.FF_RIFLE).nameEn,
            titleBn = weapons.getValue(WeaponType.FF_RIFLE).nameBn,
            subtitleEn = "Balanced Mid-Range Assault Rifle",
            subtitleBn = "মাঝারি দূরত্বের ব্যালেন্সড রাইফেল",
            modelAsset = weapons.getValue(WeaponType.FF_RIFLE).modelAsset,
            category = "WEAPON",
            weaponSpec = weapons.getValue(WeaponType.FF_RIFLE)
        ),
        ArmoryModelItem(
            id = "audi",
            titleEn = vehicles.getValue(VehicleType.AUDI_R15).nameEn,
            titleBn = vehicles.getValue(VehicleType.AUDI_R15).nameBn,
            subtitleEn = "Top Speed 151 km/h • Racing Chassis",
            subtitleBn = "সর্বোচ্চ গতি • দ্রুত চলাফেরা",
            modelAsset = vehicles.getValue(VehicleType.AUDI_R15).modelAsset,
            category = "VEHICLE",
            vehicleSpec = vehicles.getValue(VehicleType.AUDI_R15)
        ),
        ArmoryModelItem(
            id = "bmw",
            titleEn = vehicles.getValue(VehicleType.BMW_I8).nameEn,
            titleBn = vehicles.getValue(VehicleType.BMW_I8).nameBn,
            subtitleEn = "Top Speed 137 km/h • Agile Handling",
            subtitleBn = "দ্রুত ও মসৃণ স্পোর্টস গাড়ি",
            modelAsset = vehicles.getValue(VehicleType.BMW_I8).modelAsset,
            category = "VEHICLE",
            vehicleSpec = vehicles.getValue(VehicleType.BMW_I8)
        ),
        ArmoryModelItem(
            id = "dvergr",
            titleEn = vehicles.getValue(VehicleType.AJF12_DVERGR).nameEn,
            titleBn = vehicles.getValue(VehicleType.AJF12_DVERGR).nameBn,
            subtitleEn = "600 Armor HP • Heavy Tactical Transport",
            subtitleBn = "বেশি স্বাস্থ্য ও কম গতি • ভারী যান",
            modelAsset = vehicles.getValue(VehicleType.AJF12_DVERGR).modelAsset,
            category = "VEHICLE",
            vehicleSpec = vehicles.getValue(VehicleType.AJF12_DVERGR)
        ),
        ArmoryModelItem(
            id = "plane",
            titleEn = "Drop Plane Cabin",
            titleBn = "ড্রপ প্লেন কেবিন",
            subtitleEn = "100-Player Deployment Aircraft",
            subtitleBn = "লাফানোর আগের প্লেনের ভেতরের দৃশ্য",
            modelAsset = MODEL_PLANE_CABIN,
            category = "AIRCRAFT"
        )
    )
}

object L10n {
    fun pick(lang: AppLanguage, en: String, bn: String): String =
        if (lang == AppLanguage.BENGALI) bn else en
}
