# Battle Royale 3D (Android Kotlin + Jetpack Compose + Filament)

An Android 3D third-person and first-person Battle Royale game built with Kotlin, Jetpack Compose, Google Filament (`gltfio`), OkHttp WebSockets, and Jetpack DataStore.

## Implemented Features
- **100-Player Battleground**: Plane cabin drop (`plane_cabin__freefire.glb`), freefall & parachute landing, loot collection, and shrinking multi-phase Safe Zone.
- **3D Theme Park Map & Zones**: Ride Area, Parking Lot, Central Plaza, BD Sector, and Park Outskirts with zone-specific loot tables and distance-based LOD culling.
- **Weapon System**:
  - `free_fire_awm_high_texture.glb` (AWM Sniper Rifle with 8x ADS scope, high damage, slow reload)
  - `mp40_cobra_evo_gun.glb` (MP40 Cobra Evo rapid-fire SMG)
  - `free_fire_gun.glb` (FF Tactical Rifle & BD Tactical Sidearm)
  - Full stats: Damage, Range, Fire Rate, Magazine Size, Recoil, Reload Time.
- **Vehicles**:
  - `2009_audi_r15_tdi.glb` (High-speed racing prototype)
  - `bmw_i8.glb` (Agile sports coupe)
  - `ajf-12_dvergr.glb` (Heavy armored transport)
  - Includes Speed, Vehicle Health, Fuel consumption, and Door Open/Close toggle.
- **Character & Fair Combat**: `all_freefire_rare_jersey_for_free_obj_file.glb` skin with 100 Health, separate Shield bar, and no special character abilities.
- **Controls**: Mobile touch virtual joystick + Fire/Aim/Reload/Jump/Loot/Door buttons, plus external keyboard/mouse support (`WASD`, `Space`, `R`, `E`, `F`, `V`, `1/2/3`).
- **Networking**: OkHttp WebSocket client (`MultiplayerClient`) with configurable server URL in Settings and local 100-combatant arena fallback.
- **UI**: Main Lobby (3D showcase + Play, Inventory, Settings), 3D Armory Inspector, Settings (English / বাংলা, Sensitivity, Graphics LOD, WebSocket URL), and full In-Game HUD with tactical Canvas mini-map.
